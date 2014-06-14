package org.tinydvr.config

import com.typesafe.config.ConfigFactory

object ConfigurationLoader {

  private val CONFIG_FILE_PROPERTY = "tinydvr.user.configuration.file"
  private val CONFIG_FILE_DEFAULT = "/etc/tinydvr/tinydvr.conf"

  def load(): Configuration =  new Configuration {

    //
    // Load the configuration file from disk
    //
    private val configurationFile = Option(System.getProperty(CONFIG_FILE_PROPERTY)).getOrElse(CONFIG_FILE_DEFAULT)
    private val factory = ConfigFactory.load(configurationFile)

    // Authentication credientials for schedules direct.
    def schedulesDirectCredientials: SchedulesDirectCredentials = {
      val sd = factory.getConfig("schedules_direct")
      SchedulesDirectCredentials(
        sd.getString("username"),
        sd.getString("password")
      )
    }

  }

}
