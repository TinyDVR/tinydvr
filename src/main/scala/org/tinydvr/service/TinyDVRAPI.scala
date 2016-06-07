package org.tinydvr.service

import org.joda.time.DateTime
import org.squeryl.PrimitiveTypeMode._
import org.tinydvr.config.{Configured, StaticConfiguration}
import org.tinydvr.db._

trait TinyDVRAPI extends Configured {
  lazy val tinyDVRApi = new TinyDVRAPIImpl(staticConfig)
}

class TinyDVRAPIImpl(val staticConfig: StaticConfiguration) extends TinyDVRDB  {

  def findScheduledPrograms(start: DateTime, end: DateTime): List[ScheduledProgramsResponse] = {
    scheduleResultsToDTO(
      tinyDvrDb.findScheduledPrograms(start, end),
      tinyDvrDb.findRecordingsBetween(start, end)
    )
  }

  /**
   * Schedules a program to be recorded.
   */
  def scheduleRecording(stationId: String, programId: String, airDateTime: DateTime): Long = {
    // get the database objects
    val (schedule, programDTO) =
      (for {
        schedule <- tinyDvrDb.findSchedule(stationId, programId, airDateTime)
        program <- tinyDvrDb.findProgram(programId)
      } yield (schedule, program)).getOrElse {
        throw new IllegalArgumentException(s"No scheduled programs found for $stationId, $programId, $airDateTime")
      }

    // ensure this isn't already scheduled to record
    tinyDvrDb.findRecording(stationId, programId, airDateTime).foreach(r => {
      throw new IllegalArgumentException(s"Recording ${r.id} already scheduled for station $stationId and program $programId at $airDateTime")
    })

    // create the recording
    val r = new Recording
    r.durationInSeconds = schedule.durationInSeconds
    r.program = programDTO.program
    r.programId = programId
    r.searchableTitle = programDTO.searchableTitle
    r.stationId = stationId
    r.startDateTime = schedule.airDateTime

    tinyDvrDb.insertRecording(r)
  }

  def searchForUpcomingPrograms(query: String): List[(Schedule, Program)] = {
    tinyDvrDb.findScheduledPrograms(query, Some(new DateTime))
  }


  private def toDTO(dt: DateTime): DateTimeDTO = {
    DateTimeDTO(
      dt.getMillis,
      dt,
      dt.toLocalDate,
      dt.toLocalTime
    )
  }

  private def toDTO(s: Station): StationDTO = {
    StationDTO(
      s.id,
      s.info,
      AntennaTuningInfo(
        s.uhfVhf,
        s.atscMajor,
        s.atscMinor
      )
    )
  }

  private def scheduleResultsToDTO(scheduledPrograms: List[(Schedule, Program)], recordings: List[Recording]): List[ScheduledProgramsResponse] = {
    val stationsById = tinyDvrDb.findAllStations.map(s => (s.id, s)).toMap
    val recordingsByScheduleId = recordings.map(r => {
      compositeKey(r.stationId, r.programId, r.startDateTimeEpoch)
    }).toSet

    val programsByStationId =
      (for {
        (schedule, program) <- scheduledPrograms
      } yield {
        ScheduledProgram(
          toDTO(schedule.airDateTime),
          program.id,
          program.program,
          schedule.stationId,
          schedule.durationInSeconds,
          recordingsByScheduleId.contains(schedule.id)
        )
      }).groupBy(_.stationID)

    (for {
      (stationId, programs) <- programsByStationId
      station <- stationsById.get(stationId)
    } yield {
      ScheduledProgramsResponse(toDTO(station), programs)
    }).toList
  }
}
