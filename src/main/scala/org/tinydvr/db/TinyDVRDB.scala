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
  val configuration = table[Configuration]("config")
  val programs = table[Program]("programs")
  val recordings = table[Recording]("recordings")
  val schedules = table[Schedule]("schedules")
  val stations = table[Station]("stations")

  //
  // Table constraints
  //

  on(configuration)(c => declare(
    c.key is indexed,
    c.value is dbType("text")
  ))

  on(programs)(p => declare(
    p.id is indexed,
    p.md5 is indexed,
    p.programTitle is indexed,
    p.programJson is dbType("text")
  ))

  on(recordings)(r => declare(
    r.id is autoIncremented,
    r.id is indexed,
    r.status is indexed,
    r.startDateTimeEpoch is indexed,
    r.programTitle is indexed,
    r.programJson is dbType("text"),
    r.error is dbType("text"),
    r.fileName is dbType("text")
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
  // Configuration Management
  //

  def findConfiguration(key: String): Option[String] = {
    run {
      configuration.lookup(key).map(_.value)
    }
  }

  def insertOrUpdateConfiguration(key: String, value: String): Unit = {
    run {
      if (configuration.lookup(key).isDefined) {
        update(configuration)(c => {
          where(c.key === key).
            set(c.value := value)
        })
      } else {
        val c = new Configuration
        c.key = key
        c.value = value
        configuration.insert(c)
      }
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

  def findProgram(id: String): Option[Program] = {
    run {
      programs.lookup(id)
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
  // Recordings Management
  //

  /**
   * Indicates that the specific recording has started.
   * Two things happen here:
   *  1) The status is changed to 'in progress'
   *  2) The output file is populated.
   */
  def beginRecording(recordingId: Long, fileName: String): Unit = {
    run {
      update(recordings)(r => {
        where(r.id === recordingId).
          set(r.fileName := Some(fileName),
            r.status := RecordingStatus.InProgress)
      })
    }
  }

  def setRecordingSuccessful(recordingId: Long): Unit = {
    run {
      update(recordings)(r => {
        where(r.id === recordingId).
          set(r.status := RecordingStatus.Successful)
      })
    }
  }

  def setRecordingFailed(recordingId: Long, error: Option[String]): Unit = {
    run {
      update(recordings)(r => {
        where(r.id === recordingId).
          set(r.status := RecordingStatus.Failed,
            r.error := error)
      })
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

  def findStation(id: String): Option[Station] = {
    run {
      stations.lookup(id)
    }
  }


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