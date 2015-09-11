package org.tinydvr.db

import net.liftweb.json.Extraction._
import net.liftweb.json._
import org.joda.time.{LocalDate, DateTime}
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Schema
import org.tinydvr.config.{Configured, DatabaseConnectionInfo}
import org.tinydvr.schedulesdirect.api.{DateTimeSerializer, LocalDateSerializer}
import org.tinydvr.util.DatabaseConnection

object TinyDVRDB extends Schema {

  //
  // Tables
  //
  val programs = table[Program]("programs")
  val recordings = table[Recording]("recordings")
  val schedules = table[Schedule]("schedules")
  val stations = table[Station]("stations")

  //
  // Table constraints
  //

  on(programs)(p => declare(
    p.id is indexed,
    p.md5 is indexed,
    p.programTitle is indexed,
    p.programJson is dbType("text")
  ))

  on(recordings)(r => declare(
    r.id is autoIncremented,
    r.id is indexed,
    r.startDateTimeEpoch is indexed,
    r.programTitle is indexed
  ))

  on(schedules)(s => declare(
    columns(s.stationId, s.programId, s.airDateTimeEpoch) are unique,
    s.airDateTimeEpoch is indexed,
    s.stationId is indexed
  ))

  on(stations)(s => declare(
    s.id is indexed,
    s.infoJson is dbType("text")
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

trait TinyDVRDB extends Configured {
  lazy val tinyDvrDb = new TinyDVRDBAPI(staticConfig.databaseInfo)
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
  // Program Management
  //

  /**
   * Inserts the provided stations. If they already exist, it is replaced by the provided one.
   */
  def insertOrReplaceProgram(ps: Iterable[Program]): Unit = {
    if (ps.nonEmpty) {
      val existingIds = findProgramsByIds(ps.map(_.id).toSet).map(_.id).toSet
      run {
        inTransaction {
          if (existingIds.nonEmpty) {
            programs.deleteWhere(_.id in existingIds)
          }
          programs.insert(ps)
        }
      }
    }
  }

  /**
   * Returns the programs with the md5s provided.
   */
  def findProgramsByIds(ids: Set[String]): List[Program] = {
    run {
      from(programs)(p => {
        where(p.id in ids).select(p)
      }).toList
    }
  }


  /**
   * Returns the programs with the md5s provided.
   */
  def findProgramsByMd5s(md5s: Set[String]): List[Program] = {
    run {
      from(programs)(p => {
        where(p.md5 in md5s).select(p)
      }).toList
    }
  }

  /**
   * Erases programs that haven't been seen since the provided date time.
   * Note: You should be liberal here. If you purge too many programs, the next update will
   * hammer schedules direct and they will hate you.
   * @param dt  The datetime for which programs not updated since will be deleted.
   */
  def eraseProgramsNotSeenSince(dt: DateTime): Int = {
    val epoch = dt.getMillis
    run {
      inTransaction {
        programs.deleteWhere(_.lastUpdated lt epoch)
      }
    }
  }

  //
  // Schedules Management
  //

  def findAllSchedules(): List[Schedule] = {
    run {
      from(schedules)(s => select(s)).toList
    }
  }

  /**
   * Deletes schedules for programs that air before the provided datetime.
   * Note: give yourself some wiggle room here: A program may start at 11pm on Monday
   * and not end until 3am on Tuesday.
   * Deletion is done based on airdatetime since it is indexed in the database.
   * @return  The number of rows erased.
   */
  def eraseSchedulesAiringBefore(dateTime: DateTime): Int = {
    val epoch = dateTime.getMillis
    run {
      inTransaction {
        schedules.deleteWhere(_.airDateTimeEpoch lt epoch)
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
  def findScheduledPrograms(start: DateTime, end: DateTime): List[(Schedule, Program)] = {
    val startEpoch = start.getMillis
    val endEpoch = end.getMillis
    run {
      join(schedules, programs)((s, p) => {
        where(
          (s.airDateTimeEpoch lte endEpoch) and
            ((s.airDateTimeEpoch + s.durationInSeconds * 1000L) gte startEpoch)
        ).select((s, p)).on(s.programId === p.id)
      }).toList
    }
  }

  /**
   * Queries for listings containing the provided text.
   */
  def findScheduledPrograms(query: String): List[(Schedule, Program)] = {
    run {
      join(schedules, programs)((s, p) => {
        where(
          (p.programTitle like query)
        ).select((s, p)).on(s.programId === p.id)
      }).toList
    }
  }

  def replaceSchedulesForDay(day: LocalDate, ss: Iterable[Schedule]): Unit = {
    val start = day.toDateTimeAtStartOfDay.getMillis
    val end = day.toDateTimeAtStartOfDay.plusDays(1).getMillis
    assert(ss.forall(s => (s.airDateTimeEpoch < end) && (s.airDateTimeEpoch >= start)))
    run {
      inTransaction {
        schedules.deleteWhere(s => {
          (s.airDateTimeEpoch lt end) and (s.airDateTimeEpoch gte start)
        })
        schedules.insert(ss)
      }
    }
  }

  //
  // Stations Management
  //

  def findAllStations(): List[Station] = {
    run {
      from(stations)(s => select(s)).toList
    }
  }

  def replaceAllStations(ss: Iterable[Station]): Unit = {
    run {
      inTransaction {
        stations.deleteWhere(_ => 1 === 1)
        stations.insert(ss)
      }
    }
  }

}