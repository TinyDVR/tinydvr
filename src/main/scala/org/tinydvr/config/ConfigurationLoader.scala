package org.tinydvr.config

import com.typesafe.config.ConfigFactory
import java.io.File

object ConfigurationLoader {

  private val CONFIG_FILE_PROPERTY = "tinydvr.configuration.file"
  private val CONFIG_FILE_DEFAULT = "/etc/tinydvr/tinydvr.conf"

  def load(): Configuration =  new Configuration {

    //
    // Load the configuration file from disk
    //
    private val configurationFile = Option(System.getProperty(CONFIG_FILE_PROPERTY)).getOrElse(CONFIG_FILE_DEFAULT)
    private val factory = ConfigFactory.parseFile(new File(configurationFile))

    // The database connection information
    def databaseInfo: DatabaseConnectionInfo = {
      val db = factory.getConfig("db")
      DatabaseConnectionInfo(
        db.getString("url"),
        db.getString("username"),
        db.getString("password")
      )
    }

    // Authentication credentials for schedules direct.
    def schedulesDirectCredentials: SchedulesDirectCredentials = {
      val sd = factory.getConfig("schedules_direct")
      SchedulesDirectCredentials(
        sd.getString("username"),
        sd.getString("password")
      )
    }

  }

}
