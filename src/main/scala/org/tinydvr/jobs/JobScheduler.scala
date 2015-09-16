package org.tinydvr.jobs

import org.joda.time.{Period, DateTime}
import org.slf4j.LoggerFactory
import org.tinydvr.config.{Configured, StaticConfiguration}
import org.tinydvr.util.Singletons
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object JobScheduler {
  private var exector = Option.empty[JobExecutor]
  def initialize(staticConfig: StaticConfiguration): Unit = {
    this.synchronized {
      if (exector.isDefined) throw new RuntimeException("The job scheduler was already initialized.")
      val je = new JobExecutor(staticConfig)
      val scheduler = Singletons.actorSystem.scheduler

      // make sure the database has been created
      CheckDatabaseCreatedJob(staticConfig).run()

      // schedule update checks for stations
      scheduler.schedule(Duration.Zero, 1 hours)(je.checkStationsUpdate _)

      // schedule update check for listings
      scheduler.schedule(Duration.Zero, 1 hours)(je.checkListingUpdate _)

      exector = Some(je)
    }
  }
}

private class JobExecutor(val staticConfig: StaticConfiguration) extends Configured {
  private val logger = LoggerFactory.getLogger(getClass)

  def checkStationsUpdate: Unit = {
    runIfOverdue(dynamicConfig.lastStationUpdate, staticConfig.updateFrequencies.stations) {
      logger.info("Updating Stations...")
      UpdateStationsJob(staticConfig).run()
      dynamicConfig.lastStationUpdate = new DateTime
    }
  }

  def checkListingUpdate: Unit = {
    runIfOverdue(dynamicConfig.lastListingsUpdate, staticConfig.updateFrequencies.listings) {
      logger.info("Updating Listings....")
      UpdateSchedulesJob(staticConfig).run()
      UpdateProgramsJob(staticConfig).run()
      dynamicConfig.lastListingsUpdate =  new DateTime
    }
  }

  private def runIfOverdue(dt: Option[DateTime], frequencyInHours: Int)(f: => Unit): Unit = {
    val frequencyInMilliseconds = frequencyInHours * 60 * 60 * 1000L
    if (!dt.exists(date => {
      val period = new Period(date, new DateTime)
      period.getMillis <= frequencyInMilliseconds
    })) f
  }

}