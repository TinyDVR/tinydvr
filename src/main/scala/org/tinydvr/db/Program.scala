package org.tinydvr.db

import org.squeryl.KeyedEntity
import org.squeryl.annotations._
import org.joda.time.DateTime
import org.tinydvr.schedulesdirect.api.ProgramResponse

class Program extends KeyedEntity[String] {

  @Column(name = "id")  // the program id provided by schedules direct
  var id: String = _

  @Column(name = "md5") // program md5 provided by schedules direct
  var md5: String = _

  @Column(name = "last_updated")
  var lastUpdated: Long = _

  // The program title is stored here for searching
  @Column(name = "program_title", length = 255)
  var programTitle: String = _

  //
  // The json returned from schedules direct.
  //
  @Column(name = "program_json")
  var programJson: String = _

  //
  // Convenience functions for accessing the fields
  //

  def program: ProgramResponse = TinyDVRDB.fromJson[ProgramResponse](programJson)

  def program_=(r: ProgramResponse): Unit = {
    programJson = TinyDVRDB.toJson(r)
  }

}
