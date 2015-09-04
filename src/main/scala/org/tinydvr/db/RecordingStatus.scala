package org.tinydvr.db

object RecordingStatus extends Enumeration {
  type RecordingStatus = Value

  val Scheduled = Value(0, "Scheduled") // The recording will happen in the future.
  val InProgress = Value(1, "InProgress") // The program is being recorded
  val Successful = Value(2, "Successful") // The recording was successful
  val Failed = Value(3, "Failed") // The recording failed
}