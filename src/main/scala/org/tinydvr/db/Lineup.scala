package org.tinydvr.db

import org.squeryl.KeyedEntity
import org.squeryl.annotations._
import org.joda.time.DateTime
import org.tinydvr.schedulesdirect.api.GetLineupResponse

class Lineup extends KeyedEntity[Long] {

  @Column(name = "id")
  var id: Long = _

  @Column(name = "uri", length = 255)
  var uri: String = _

  //
  // The epoch for the last time that this lineup was updated
  //
  @Column(name = "last_updated")
  var lastUpdatedEpoch: Long = _

  //
  // The json returned from schedules direct.
  //
  @Column(name = "lineup_json")
  var lineupJson: String = _

  //
  // Convenience functions for accessing the fields
  //

  def lastUpdated: DateTime = {
    new DateTime(lastUpdatedEpoch)
  }

  def lastUpdated_=(dt: DateTime): Unit = {
    lastUpdatedEpoch = dt.getMillis
  }

  def lineup: GetLineupResponse = {
    TinyDVRDB.fromJson[GetLineupResponse](lineupJson)
  }

  def lineup_=(r: GetLineupResponse): Unit = {
    lineupJson = TinyDVRDB.toJson(r)
  }

}
