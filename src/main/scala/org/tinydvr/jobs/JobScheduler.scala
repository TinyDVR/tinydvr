package org.tinydvr.jobs

import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import org.tinydvr.config._
import org.tinydvr.db.{TinyDVRDB, Recording}
import org.tinydvr.util.Singletons
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.blocking

object JobScheduler {
  private var exector = Option.empty[JobExecutor]
  private val scheduler = Singletons.actorSystem.scheduler
  def initialize(configuration: TinyDvrConfiguration): Unit = {
    this.synchronized {
      if (exector.isDefined) throw new RuntimeException("The job scheduler was already initialized.")
      val je = new JobExecutor(configuration)

      // make sure the database has been created
      CheckDatabaseCreatedJob(configuration).run()

      // schedule update checks for stations
      scheduler.schedule(Duration.Zero, 1 hours)(je.checkStationsUpdate)

      // schedule update check for listings
      scheduler.schedule(1 minutes, 1 hours)(je.checkListingUpdate)

      // check to see if we have any recordings to execute
      scheduler.schedule(Duration.Zero, 333 milliseconds)(je.recordOverduePrograms)

      exector = Some(je)
    }
  }
}

private class JobExecutor(val tinyDvrConfiguration: TinyDvrConfiguration)
  extends Configured with TinyDVRDB {
  private val futures = scala.collection.mutable.HashSet[Future[Unit]]()
  private val logger = LoggerFactory.getLogger(getClass)

  def checkStationsUpdate(): Unit = {
    runIfOverdue(tinyDvrConfiguration.lastStationUpdate, tinyDvrConfiguration.listings.updateStationFrequencyInHours) {
      logger.info("Updating Stations...")
      UpdateStationsJob(tinyDvrConfiguration).run()
    }
  }

  def checkListingUpdate(): Unit = {
    runIfOverdue(tinyDvrConfiguration.lastListingsUpdate, tinyDvrConfiguration.listings.updateListingsFrequencyInHours) {
      logger.info("Updating Listings....")
      UpdateSchedulesJob(tinyDvrConfiguration).run()
      UpdateProgramsJob(tinyDvrConfiguration).run()
    }
  }

  def recordOverduePrograms(): Unit = {
    tinyDvrDb.findOverdueRecordings.foreach(recordProgram)
  }

  private def recordProgram(recording: Recording): Unit = {
    val future = Future {
      blocking {
        RecordJob(recording, tinyDvrConfiguration).run()
      }
    }
    futures += future
    future.onComplete {
      case _ => futures -= future
    }
  }

  private def runIfOverdue(dt: Option[DateTime], frequencyInHours: Int)(f: => Unit): Unit = {
    val frequencyInMilliseconds = frequencyInHours * 60 * 60 * 1000L
    if (dt.isEmpty || dt.exists(date => {
      val diff = (new DateTime).getMillis - date.getMillis
      diff >= frequencyInMilliseconds
    })) f
  }

}