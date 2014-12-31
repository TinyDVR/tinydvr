package org.tinydvr.util

import org.tinydvr.schedulesdirect.api._
import org.tinydvr.schedulesdirect.api.Implicits._
import org.tinydvr.config.SchedulesDirectCredentials
import org.slf4j.LoggerFactory

trait SchedulesDirectSupport {

  private lazy val logger = LoggerFactory.getLogger(getClass)

  /**
   * Gets a schedules direct api client using a cached token if possible.
   * If the cached token is invalid, the client is reauthorized using the provided credentials.
   * @param creds   The schedules direct credentials
   * @return        A new SchedulesDirectAPIClient
   * @throws        SchedulesDirectOfflineException if the schedules direct service is offline.
   */
  protected def getClient(creds: SchedulesDirectCredentials): SchedulesDirectAPIClient = {
    SDCache.token.map(token => {
      val client = new SchedulesDirectAPIClient(token)
      try {
        checkStatus(client)
        client
      } catch {
        case u: InvalidUserException => { // the token is no longer valid
          logger.warn("The current schedules direct token is invalid; attempting to re-authorize.")
          SDCache.token = None
          getClient(creds)
        }
      }
    }).getOrElse {
      val token = SchedulesDirectAuthenticator.getToken(creds.username, creds.password).getResult.token
      SDCache.token = Some(token)
      val client = new SchedulesDirectAPIClient(token)
      checkStatus(client)
      client
    }
  }

  private def checkStatus(client: SchedulesDirectAPIClient): Unit = {
    val currentStatus = client.getStatus.getResult.systemStatus.maxBy(_.date.getMillis)
    if (currentStatus.status != "Online") {
      throw SchedulesDirectOfflineException(currentStatus)
    }
  }

}

case class SchedulesDirectOfflineException(status: SystemStatus) extends Exception("Schedules Direct is " + status.status)

private object SDCache {
  var token: Option[String] = None
}