package org.tinydvr.db

import org.squeryl.KeyedEntity
import org.squeryl.annotations._
import org.tinydvr.schedulesdirect.api.LineupStationInfo

class Station extends KeyedEntity[String] {

  @Column(name = "id")  // the station id provided by schedules direct
  var id: String = _

  //
  // Tuning info
  //

  @Column(name = "uhfVhf")
  var uhfVhf: Int = _

  @Column(name = "atscMajor")
  var atscMajor: Int = _

  @Column(name = "atscMinor")
  var atscMinor: Int = _

  @Column(name = "info_json") // station info from schedules direct
  var infoJson: String = _

  //
  // Convenience functions for accessing the fields
  //

  def info: LineupStationInfo = TinyDVRDB.fromJson[LineupStationInfo](infoJson)

  def info_=(i: LineupStationInfo): Unit = {
    infoJson = TinyDVRDB.toJson(i)
  }

}
