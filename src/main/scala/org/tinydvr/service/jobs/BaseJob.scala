package org.tinydvr.service.jobs

import org.tinydvr.config.{DatabaseConnectionInfo, Configured}
import org.tinydvr.db.TinyDVRDB
import org.tinydvr.schedulesdirect.api._
import org.tinydvr.util.SchedulesDirectSupport
import org.slf4j.LoggerFactory

/**
 * Provides access to common resources for execution tasks.
 */
trait BaseJob extends Configured with TinyDVRDB with SchedulesDirectSupport {

  import org.tinydvr.schedulesdirect.api.Implicits._

  protected def dbInfo: DatabaseConnectionInfo = config.databaseInfo

  //
  // All jobs should implement this method.
  //
  protected def run(): Unit

  /**
   * A convenience function that runs this job and:
   * 1) Times the execution
   * 2) Catches any errors
   * 3) Prints the results of (1) and (2) to the logger.
   */
  def execute(): Unit = {
    try {
      timed("Job %s".format(jobName)) {
        run
      }
    } catch {
      case e: Exception => {
        logger.error("Could not execute %s, caught".format(jobName), e)
      }
    }
  }

  //
  // Useful helper values for jobs
  //

  protected lazy val logger = LoggerFactory.getLogger(getClass)


  //
  // Useful helper functions for jobs
  //

  /**
   * @return  The name of this class
   */
  protected def jobName: String = getClass.getName.split("\\.").last

  /**
   * @return  A new instance of the schedules direct client with a new API client.
   */
  protected def schedulesDirectApi(): SchedulesDirectAPIClient = {
    val creds = config.schedulesDirectCredentials
    val token = SchedulesDirectAuthenticator.getToken(creds.username, creds.password)
    new SchedulesDirectAPIClient(token.getResult.token)
  }

  /**
   * Times the provided method and prints the result to logger.info
   */
  protected def timed[T](msg: String)(body: => T): T = {
    val sw = System.currentTimeMillis
    try {
      body
    } finally {
      logger.info("%s took %d milliseconds".format(msg, System.currentTimeMillis - sw))
    }
  }
}
