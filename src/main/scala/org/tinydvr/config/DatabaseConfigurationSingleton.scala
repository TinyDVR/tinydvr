package org.tinydvr.config

import com.typesafe.config.ConfigFactory
import java.io.File

object DatabaseConfigurationSingleton {

  private val CONFIG_FILE_PROPERTY = "tinydvr.configuration.file"
  private val CONFIG_FILE_DEFAULT = "/etc/tinydvr/tinydvr.conf"

  lazy val fromProperties: DatabaseConnectionInfo =  {

    //
    // Load the configuration file from disk
    //
    val configurationFile = Option(System.getProperty(CONFIG_FILE_PROPERTY)).getOrElse(CONFIG_FILE_DEFAULT)
    val factory = ConfigFactory.parseFile(new File(configurationFile))

    // The database connection information

    val db = factory.getConfig("db")
    DatabaseConnectionInfo(
      db.getString("url"),
      db.getString("username"),
      db.getString("password")
    )
  }

}
