package org.tinydvr.jobs

import java.io.File
import org.tinydvr.config.StaticConfiguration
import org.tinydvr.db.Recording
import org.tinydvr.util.VariableReplacer
import scala.sys.process._

case class RecordJob(recording: Recording, val staticConfig: StaticConfiguration) extends BaseJob with VariableReplacer {

  override protected def runInternal(): Unit = {
    try {

      logger.info(s"Looking up needed info for recording ${recording.id}")
      // look up the program and station info
      val program = tinyDvrDb.findProgram(recording.programId).getOrElse {
        throw new RuntimeException(s"Could not find program ${recording.programId} for recording ${recording.id}.")
      }
      val station = tinyDvrDb.findStation(recording.stationId).getOrElse {
        throw new RuntimeException(s"Could not find station ${recording.stationId} for recording ${recording.id}.")
      }
      // replace any variables in the configuration
      val strings = staticConfig.recordings.directory :: staticConfig.recordings.fileName :: staticConfig.tuner.arguments
      val (arguments, outputFile) = variableReplacer.replace(strings, station, program, recording) match {
        case path :: filename :: args => {
          (new File(path)).mkdirs
          (args, path + "/" + filename)
        }
      }

      // indicate that the recording has begin
      tinyDvrDb.beginRecording(recording.id, outputFile)

      // execute the recording
      val cmd = (staticConfig.tuner.executable :: arguments) :+ outputFile
      logger.info("Executing " + cmd.mkString(" "))
      val pl = ProcessLogger(logger.info, logger.error)
      if (cmd.!(pl) != 0) {
        throw new Exception("Recording returned non-zero status, see log for stderr.")
      }

      // record great success
      tinyDvrDb.setRecordingSuccessful(recording.id)
    } catch {
      case e: Exception => {
        logger.error(s"Failed to record recording ${recording.id}", e)
        tinyDvrDb.setRecordingFailed(recording.id, Some(e.getMessage))
      }
    }

  }
}