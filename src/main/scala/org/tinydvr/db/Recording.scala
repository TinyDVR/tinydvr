package org.tinydvr.db

import org.joda.time.DateTime
import org.squeryl.KeyedEntity
import org.squeryl.annotations._
import org.tinydvr.schedulesdirect.api.ProgramResponse

class Recording(
  // in constructor since squeryl gets confused about indexes enums otherwise.
  @Column(name = "status")
  var status: RecordingStatus.Value = RecordingStatus.Scheduled
) extends KeyedEntity[Long]{

  @Column(name = "id")  // auto-incremented
  var id: Long = _

  @Column(name = "station_id")
  var stationId: String = _

  @Column(name = "file_name")
  var fileName: Option[String] = None // where the recording is located. is set when recording enters 'in progress' status

  @Column(name = "error") // if the recording failed, indicate why.
  var error: Option[String] = None

  //
  // Scheduling info for the recording
  //

  // When the recording should begin.
  @Column(name = "start_date_time_epoch")
  var startDateTimeEpoch: Long = _

  // How long to record for.
  @Column(name = "duration_in_seconds")
  var durationInSeconds: Int = _


  //
  // Program information
  //

  @Column(name = "program_id")  // the program id provided by schedules direct
  var programId: String = _

  // The program title is stored here for searching and grouping
  @Column(name = "program_title", length = 255)
  var programTitle: String = _

  //
  // A copy of the full program information. It is duplicated from the
  // programs table since recordings may live on forever, and the programs table
  // is occasionally purged.
  //
  @Column(name = "program_json")
  var programJson: String = _

  //
  // Convenience functions for accessing the fields
  //

  def program: ProgramResponse = TinyDVRDB.fromJson[ProgramResponse](programJson)

  def program_=(r: ProgramResponse): Unit = {
    programJson = TinyDVRDB.toJson(r)
  }

  def startDateTime: DateTime = new DateTime(startDateTimeEpoch)

  def startDateTime_=(dt: DateTime): Unit = {
    startDateTimeEpoch = dt.getMillis
  }
}