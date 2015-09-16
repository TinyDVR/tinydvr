package org.tinydvr.config

import org.joda.time.DateTime
import org.tinydvr.db.TinyDVRDB

/**
 * A class for accessing and updating dynamic configurations stored in the database.
 * The class has a number of attributes with modifiers. The modifiers update the database.
 */
class DynamicConfiguration(val staticConfig: StaticConfiguration) extends Configured with TinyDVRDB {
  import org.tinydvr.config.DynamicConfigurationKeys._

  //
  // Scheduling configuration values.
  //

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
}

private[config] object DynamicConfigurationKeys {
  val LAST_STATION_UPDATE_TIME = "scheduling.last_station_update"
  val LAST_LISTINGS_UPDATE_TIME = "scheduling.last_listings_update"
}
