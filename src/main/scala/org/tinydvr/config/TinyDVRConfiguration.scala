package org.tinydvr.config

import net.liftweb.json._
import org.joda.time.DateTime
import org.tinydvr.db.TinyDVRDB

/**
 * A class for accessing and updating dynamic configurations stored in the database.
 * The class has a number of attributes with modifiers. The modifiers update the database.
 */
class TinyDvrConfiguration(val databaseConfiguration: DatabaseConnectionInfo) extends TinyDVRDB {
  import TinyDvrConfiguration._

  implicit val formats = DefaultFormats

  def recordings: RecordingsConfig = {
    findConfigObject[RecordingsConfig](RECORDINGS)
  }

  def recordingConfiguration_=(config: RecordingsConfig): Unit = {
    setConfigObject(RECORDINGS, config)
  }

  def schedulesDirectCredentials: SchedulesDirectCredentials = {
    findConfigObject[SchedulesDirectCredentials](SCHEDULES_DIRECT_CREDENTIALS)
  }

  def schedulesDirectCredentials_=(creds: SchedulesDirectCredentials): Unit = {
    setConfigObject(SCHEDULES_DIRECT_CREDENTIALS, creds)
  }

  def lastListingsUpdate: Option[DateTime] = {
    tinyDvrDb.findConfiguration(LAST_LISTINGS_UPDATE_TIME).map(v => new DateTime(v.toLong))
  }

  def lastListingsUpdate_=(dt: DateTime): Unit = {
    tinyDvrDb.insertOrUpdateConfiguration(LAST_LISTINGS_UPDATE_TIME, dt.getMillis.toString)
  }

  def lastStationUpdate: Option[DateTime] = {
    tinyDvrDb.findConfiguration(LAST_STATION_UPDATE_TIME).map(v => new DateTime(v.toLong))
  }

  def lastStationUpdate_=(dt: DateTime): Unit = {
    tinyDvrDb.insertOrUpdateConfiguration(LAST_STATION_UPDATE_TIME, dt.getMillis.toString)
  }

  def listings: ListingsConfiguration = {
    findConfigObject[ListingsConfiguration](LISTINGS)
  }

  def listings_=(config: ListingsConfiguration): Unit = {
    setConfigObject(LISTINGS, config)
  }
  
  def tuner: TunerConfiguration = {
    findConfigObject[TunerConfiguration](TUNER)
  }
  
  def tuner_=(config: TunerConfiguration): Unit = {
    setConfigObject(TUNER, config)
  }


  private def findConfigObject[T](key: String)(implicit mf: scala.reflect.Manifest[T]): T = {
    tinyDvrDb.findConfiguration(key).map(JsonParser.parse(_).extract[T]).getOrElse {
      throw ConfigurationMissingException(key)
    }
  }
  
  private def setConfigObject(key: String, value: AnyRef): Unit = {
    tinyDvrDb.insertOrUpdateConfiguration(key, Serialization.write(value))
  }
}

private[config] object TinyDvrConfiguration {
  private val SCHEDULES_DIRECT_CREDENTIALS = "user.schedules_direct_credentials"
  private val LISTINGS = "listings"
  private val TUNER = "tuner"
  private val RECORDINGS = "recordings"
  private val LAST_STATION_UPDATE_TIME = "last_station_update"
  private val LAST_LISTINGS_UPDATE_TIME = "last_listings_update"
}

case class ConfigurationMissingException(key: String) extends Exception(s"No configuration for $key in the database.")