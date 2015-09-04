package org.tinydvr.jobs

import org.joda.time.{DateTime, LocalDate}
import org.tinydvr.config.StaticConfiguration
import org.tinydvr.db.Schedule
import org.tinydvr.util.SchedulesDirectAPI

case class UpdateSchedulesJob(val staticConfig: StaticConfiguration) extends BaseJob with SchedulesDirectAPI {

  import org.tinydvr.schedulesdirect.api.Implicits._

  def runInternal(): Unit = {

    // determine the days we're processing for
    val today = new LocalDate
    val dates = (0 until staticConfig.listings.fetch).map(today.plusDays).toList

    // get the scheduled programs
    val stationIds = tinyDvrDb.findAllStations.map(_.id).toSet
    logger.info(s"Downloading ${dates.size} days worth of listings for ${stationIds.size} stations.")
    val schedules = schedulesDirectAPI.getSchedules(stationIds, dates).getResult

    //
    // Update the schedules
    //
    (for {
      schedule <- schedules
      program <- schedule.programs
    } yield {
      val s = new Schedule
      s.stationId = schedule.stationID
      s.programId = program.programID
      s.programMd5 = program.md5
      s.airDateTime = program.airDateTime
      s.durationInSeconds = program.duration
      s
    }).groupBy(_.airDateTime.toLocalDate).foreach {
      case (day, schedules) => {
        logger.info(s"Updating schedules for ${day}")
        tinyDvrDb.replaceSchedulesForDay(day, schedules)
      }
    }

    // purge old programs
    val purgeDate = new DateTime().minusDays(staticConfig.listings.retain)
    logger.info(s"Erasing schedules not seen since ${purgeDate}")
    val numDeleted = tinyDvrDb.eraseSchedulesAiringBefore(purgeDate)
    logger.info(s"Erased ${numDeleted} programs.")
  }
}
