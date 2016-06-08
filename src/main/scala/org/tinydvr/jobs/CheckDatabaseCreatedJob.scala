package org.tinydvr.jobs

import org.h2.jdbc.JdbcSQLException
import org.tinydvr.config._

case class CheckDatabaseCreatedJob(val tinyDvrConfiguration: TinyDvrConfiguration) extends BaseJob {

  def runInternal(): Unit = {
    logger.info("Creating database tables.")
    try {
      // Create the tables
      tinyDvrDb.create

      // Set some default values..
      tinyDvrConfiguration.listings = ListingsConfiguration(
        updateStationFrequencyInHours =  720, // 30 days..
        updateListingsFrequencyInHours =  24,
        retainProgramsPeriodInDays = 14,
        fetchNumDaysOfListings = 3
      )
    } catch {
      case e: RuntimeException
        if (e.getCause.isInstanceOf[JdbcSQLException] && e.getCause.getMessage.contains("already exists")) => {
        logger.info("Tables already created")
      }
    }
  }
}
