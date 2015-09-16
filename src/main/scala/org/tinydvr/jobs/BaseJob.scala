package org.tinydvr.jobs

import org.slf4j.LoggerFactory
import org.tinydvr.config._
import org.tinydvr.db._

/**
 * Provides access to common resources for execution tasks.
 */
trait BaseJob extends Configured with TinyDVRDB {

  //
  // All jobs should implement this method.
  //
  protected def runInternal(): Unit

  /**
   * A convenience function that runs this job and:
   * 1) Times the execution
   * 2) Catches any errors
   * 3) Prints the results of (1) and (2) to the logger.
   * 4) Records when the job was started to the database.
   */
  def run(): Unit = {
    val log = new JobLog
    log.name = jobName
    log.timestamp = System.currentTimeMillis
    try {
      timed(jobName) {
        runInternal
      }
      log.status = JobStatus.Successful
    } catch {
      case e: Exception => {
        logger.error("Could not execute %s, caught".format(jobName), e)
        log.status = JobStatus.Failed
      }
    }
    tinyDvrDb.deleteOldLogsForJob(jobName)
    tinyDvrDb.insertJobLog(log)
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
