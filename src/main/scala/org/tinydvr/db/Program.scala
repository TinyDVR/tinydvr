package org.tinydvr.db

import org.squeryl.KeyedEntity
import org.squeryl.annotations._
import org.joda.time.DateTime
import org.tinydvr.service.api.{Program => ProgramDTO}

class Program extends KeyedEntity[String] {

  @Column(name = "id")
  var id: String = _

  //
  // The epoch for the last time that this lineup was updated
  //
  @Column(name = "last_updated_epoch")
  var lastUpdatedEpoch: Long = System.currentTimeMillis

  //
  // The json returned from schedules direct.
  //
  @Column(name = "program_json")
  var programJson: String = _

  //
  // Convenience functions for accessing the fields
  //

  def lastUpdated: DateTime = {
    new DateTime(lastUpdatedEpoch)
  }

  def lastUpdated_=(dt: DateTime): Unit = {
    lastUpdatedEpoch = dt.getMillis
  }

  def program: ProgramDTO = TinyDVRDB.fromJson[ProgramDTO](programJson)

  def lineup_=(r: ProgramDTO): Unit = {
    programJson = TinyDVRDB.toJson(r)
  }

}
