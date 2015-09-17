package org.tinydvr.service

import org.joda.time.DateTime
import org.tinydvr.config.{Configured, StaticConfiguration}
import org.tinydvr.db.{Recording, TinyDVRDB}

trait TinyDVRAPI extends Configured {
  lazy val tinyDVRApi = new TinyDVRAPIImpl(staticConfig)
}

class TinyDVRAPIImpl(val staticConfig: StaticConfiguration) extends TinyDVRDB  {

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

}
