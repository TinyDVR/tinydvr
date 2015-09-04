package org.tinydvr.config

import java.sql.{Connection, DriverManager}
import org.squeryl.adapters.H2Adapter
import org.squeryl.internals.DatabaseAdapter

case class DatabaseConnectionInfo(url: String, username: String, password: String) {
  private val H2DB_REGEX = """^jdbc:h2:.*""".r

  def getAdapter: DatabaseAdapter = {
    url match {
      case H2DB_REGEX() => new H2Adapter
      case _ => throw new IllegalArgumentException("Could not create adapter for url \"" + url + "\"")
    }
  }

  def getConnection: Connection= {
    DriverManager.getConnection(url, username, password)
  }
}

case class SchedulesDirectCredentials(username: String, password: String)