package org.tinydvr.jobs

import org.h2.jdbc.JdbcSQLException
import org.tinydvr.config.StaticConfiguration

case class CheckDatabaseCreatedJob(val staticConfig: StaticConfiguration) extends BaseJob {

  def runInternal(): Unit = {
    logger.info("Creating database tables.")
    try {
      tinyDvrDb.create
    } catch {
      case e: RuntimeException
        if (e.getCause.isInstanceOf[JdbcSQLException] && e.getCause.getMessage.contains("already exists")) => {
        logger.info("Tables already created")
      }
    }
  }
}
