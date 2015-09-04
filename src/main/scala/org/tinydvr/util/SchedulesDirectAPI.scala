package org.tinydvr.util

import org.tinydvr.config.Configured
import org.tinydvr.schedulesdirect.api.{SchedulesDirectAuthenticator, SchedulesDirectAPIClient}

trait SchedulesDirectAPI extends Configured {
  import org.tinydvr.schedulesdirect.api.Implicits._

  // TODO: make this a def and re-create if token expires.
  lazy val schedulesDirectAPI = {
    val creds = staticConfig.schedulesDirectCredentials
    val tokenResponse = SchedulesDirectAuthenticator.getToken(creds.username, creds.password).getResult
    if (tokenResponse.code != 0) throw ServiceOfflineException(tokenResponse.message)
    val token = tokenResponse.token
    new SchedulesDirectAPIClient(token)
  }
}

case class ServiceOfflineException(msg: String) extends Exception(msg)