package org.tinydvr.config

import com.typesafe.config.ConfigFactory
import java.io.File
import scala.collection.JavaConverters._

object StaticConfigurationSingleton {

  private val CONFIG_FILE_PROPERTY = "tinydvr.configuration.file"
  private val CONFIG_FILE_DEFAULT = "/etc/tinydvr/tinydvr.conf"

  lazy val fromProperties: StaticConfiguration =  {

    //
    // Load the configuration file from disk
    //
    val configurationFile = Option(System.getProperty(CONFIG_FILE_PROPERTY)).getOrElse(CONFIG_FILE_DEFAULT)
    val factory = ConfigFactory.parseFile(new File(configurationFile))

    // The database connection information
    val databaseInfo = {
      val db = factory.getConfig("db")
      DatabaseConnectionInfo(
        db.getString("url"),
        db.getString("username"),
        db.getString("password")
      )
    }

    // Authentication credentials for schedules direct.
    val schedulesDirectCredentials = {
      val sd = factory.getConfig("schedules_direct")
      SchedulesDirectCredentials(
        sd.getString("username"),
        sd.getString("password")
      )
    }

    // Listing settings. These define how much data is downloaded from schedules direct.
    val listings = {
      val listings = factory.getConfig("listings")
      SchedulingListingConfiguration(
        listings.getInt("retain"),
        listings.getInt("fetch")
      )
    }

    // Update settings. These define how often data is downloaded form schedules direct.
    val updateFrequencies = {
      val updateFrequencies = factory.getConfig("update_frequencies")
      SchedulingUpdateFrequenciesConfiguration(
        updateFrequencies.getInt("stations"),
        updateFrequencies.getInt("listings")
      )
    }

    val recordingConfig = {
      val recording = factory.getConfig("recordings")
      RecordingConfig(
        recording.getString("directory"),
        recording.getString("file_name")
      )
    }

    val tuner = {
      val tuner = factory.getConfig("tuner")
      TunerConfig(
        tuner.getString("executable"),
        tuner.getStringList("arguments").asScala.toList
      )
    }

    StaticConfiguration(
      databaseInfo,
      schedulesDirectCredentials,
      listings,
      updateFrequencies,
      recordingConfig,
      tuner
    )
  }

}
