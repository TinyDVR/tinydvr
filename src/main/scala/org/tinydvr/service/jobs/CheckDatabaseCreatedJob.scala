package org.tinydvr.service.jobs

import org.h2.jdbc.JdbcSQLException

case class CheckDatabaseCreatedJob() extends BaseJob {

  def run(): Unit = {
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
