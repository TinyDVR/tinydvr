package org.tinydvr.db

import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.annotations._
import org.squeryl.dsl.CompositeKey4

class Schedule extends KeyedEntity[CompositeKey4[Long, String, String, Long]] {

  def id: CompositeKey4[Long, String, String, Long] = compositeKey(lineupId, stationId, programId, airDateTimeEpoch)

  @Column(name = "lineup_id")
  var lineupId: Long = _

  @Column(name = "station_id")
  var stationId: String = _

  @Column(name = "program_id")
  var programId: String = _

  // The program title is stored outside the json for searching
  @Column(name = "program_title", length = 255)
  var programTitle: String = _

  @Column(name = "air_date_time_epoch")
  var airDateTimeEpoch: Long = _

  @Column(name = "duration_in_seconds")
  var durationInSeconds: Int = _

  @Column(name = "is_new")
  var isNew: Boolean = _
}
