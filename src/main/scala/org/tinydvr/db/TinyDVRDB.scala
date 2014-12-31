package org.tinydvr.db

import net.liftweb.json.Extraction._
import net.liftweb.json._
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Schema
import org.tinydvr.config.DatabaseConnectionInfo
import org.tinydvr.schedulesdirect.api.{GetLineupResponse, LocalDateSerializer, DateTimeSerializer}
import org.tinydvr.util.DatabaseConnection
import org.joda.time.DateTime

object TinyDVRDB extends Schema {

  //
  // Tables
  //

  val lineups = table[Lineup]("lineups")
  val programs = table[Program]("programs")
  val schedules = table[Schedule]("schedules")

  //
  // Table constraints
  //

  on(lineups)(l => declare(
    l.id is (autoIncremented),
    l.uri is (indexed),
    l.uri is (unique),
    l.lineupJson is (dbType("text"))
  ))

  on(programs)(p => declare(
    p.id is (indexed),
    p.programJson is (dbType("text"))
  ))

  on(schedules)(s => declare(
    columns(s.lineupId, s.stationId, s.programId, s.airDateTimeEpoch) are (unique),
    columns(s.lineupId, s.airDateTimeEpoch) are (indexed),
    columns(s.lineupId, s.stationId) are (indexed),
    columns(s.lineupId, s.programTitle) are (indexed)
  ))

  //
  // Some useful constants and helpers
  //

  val schedulesDirectFormats =
    DefaultFormats +
      new DateTimeSerializer +
      new LocalDateSerializer

  def fromJson[T](is: String)(implicit mf: scala.reflect.Manifest[T]): T = {
    implicit val formats = schedulesDirectFormats
    JsonParser.parse(is).extract[T]
  }

  def toJson[T](t: T): String = {
    implicit val formats = schedulesDirectFormats
    compact(render(decompose(t)))
  }
}

trait TinyDVRDB {
  protected def dbInfo: DatabaseConnectionInfo

  lazy val tinyDvrDb = new TinyDVRDBAPI(dbInfo)
}

class TinyDVRDBAPI(db: DatabaseConnectionInfo) extends DatabaseConnection(db) {

  import TinyDVRDB._

  //
  // Database management
  //
  def create() : Unit = {
    run {
      TinyDVRDB.create
    }
  }

  //
  // Lineup Management
  //

  def getAllLineups(): List[Lineup] = {
    run {
      from(lineups)(l => select(l)).toList
    }
  }

  def getLineup(uri: String): Option[Lineup] = {
    run {
      from(lineups)(l => {
        where(l.uri === uri).select(l)
      }).headOption
    }
  }

  /**
   * Inserts or updates the provided lineup information.
   * @param uri     The unique uri of the lineup for schedules direct.
   * @param lineup  The lineup data returned by schedules direct
   * @return        The lineup id
   */
  def insertOrUpdateLineup(uri: String, lineup: GetLineupResponse): Long = {
    run {
      val existing = getLineup(uri)
      val record = existing.getOrElse {
        val l = new Lineup
        l.uri = uri
        l
      }
      record.lastUpdated = new DateTime
      record.lineup = lineup
      if (existing.isDefined) {
        lineups.update(record)
      } else {
        lineups.insert(record)
      }
      record.id
    }
  }

  //
  // Program Management
  //

  /**
   * Inserts new programs into the data base.
   */
  def insertPrograms(ps: Iterable[Program]): Unit = {
    run {
      inTransaction {
        programs.insert(ps)
      }
    }
  }

  /**
   * Returns all program ids currently in the database.
   * Note that schedules direct requires applications to cache programs to avoid hammering their servers.
   * @return   All of the program ids currently in the database
   */
  def getAllProgramIds(): Set[String] = {
    run {
      from(programs)(p => select(p.id)).toSet
    }
  }

  /**
   * Erases programs that haven't been seen since the provided date time.
   * Note: You should be liberal here. If you purge too many programs, the next update will
   * hammer schedules direct and they will hate you.
   * @param dt  The datetime for which programs not updated since will be deleted.
   */
  def purgeProgramsNotSeenSince(dt: DateTime): Unit = {
    val epoch = dt.getMillis
    run {
      inTransaction {
        programs.deleteWhere(_.lastUpdatedEpoch lt epoch)
      }
    }
  }

  /**
   * Sets the last updated field of the provided ids to the current system time.
   * This should be run for cached program ids.
   */
  def touchPrograms(programIds: Set[String]): Unit = {
    val epoch = System.currentTimeMillis
    run {
      inTransaction {
        update(programs)(p => {
          where(p.id in programIds).
            set(p.lastUpdatedEpoch := epoch)
        })
      }
    }
  }

  //
  // Schedules Management
  //

  /**
   * Deletes schedules for programs that air before the provided datetime.
   * Note: give youself some wiggle room here: A program may start at 11pm on Monday
   * and not end until 3am on Tuesday.
   * Deletion is done based on airdatetime since it is indexed in the database.
   * @return  The number of rows erased.
   */
  def eraseSchedulesAiringBefore(lineupId: Long, dateTime: DateTime): Int = {
    val epoch = dateTime.getMillis
    run {
      inTransaction {
        schedules.deleteWhere(s => {
          (s.lineupId === lineupId) and
            (s.airDateTimeEpoch lt epoch)
        })
      }
    }
  }

  /**
   * Queries for listings between the provided date times for the provided station ids.
   * The query handles border cases:
   * A program that starts before the startTime but ends in the range will be returned.
   * Similarly, a program that ends after endTime but starts in the range will be returned.
   * This query should be relatively fast for short times (a few hours) or a single station.
   */
  def findScheduledPrograms(lineupId: Long, start: DateTime, end: DateTime): List[(Schedule, Program)] = {
    val startEpoch = start.getMillis
    val endEpoch = end.getMillis
    run {
      join(schedules, programs)((s, p) => {
        where(
          (s.lineupId === lineupId) and
            (s.airDateTimeEpoch lte endEpoch) and
            ((s.airDateTimeEpoch + s.durationInSeconds * 1000L) gte startEpoch)
        ).select((s, p)).on(s.programId === p.id)
      }).toList
    }
  }

  /**
   * Queries for listings containing the provided text.
   */
  def findScheduledPrograms(lineupId: Long, query: String): List[(Schedule, Program)] = {
    run {
      join(schedules, programs)((s, p) => {
        where(
          (s.lineupId === lineupId) and
            (s.programTitle like query)
        ).select((s, p)).on(s.programId === p.id)
      }).toList
    }
  }

  /**
   * Queries for listings for the provided station id.
   */
  def findScheduledProgramsForStation(lineupId: Long, stationId: String): List[(Schedule, Program)] = {
    run {
      join(schedules, programs)((s, p) => {
        where(
          (s.lineupId === lineupId) and
            (s.programId === stationId)
        ).select((s, p)).on(s.programId === p.id)
      }).toList
    }
  }

  def insertSchedules(s: Iterable[Schedule]): Unit = {
    run {
      inTransaction {
        schedules.insert(s)
      }
    }
  }

}