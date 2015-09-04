package org.tinydvr.db

import org.joda.time.DateTime
import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.annotations._
import org.squeryl.dsl.CompositeKey3

class Schedule extends KeyedEntity[CompositeKey3[String, String, Long]] {

  def id: CompositeKey3[String, String, Long] = compositeKey(stationId, programId, airDateTimeEpoch)

  @Column(name = "station_id")
  var stationId: String = _

  @Column(name = "program_id")
  var programId: String = _

  @Column(name = "program_md5")
  var programMd5: String = _

  @Column(name = "air_date_time_epoch")
  var airDateTimeEpoch: Long = _

  @Column(name = "duration_in_seconds")
  var durationInSeconds: Int = _

  //
  // Convenience functions for accessing the fields
  //

  def airDateTime: DateTime = new DateTime(airDateTimeEpoch)

  def airDateTime_=(dt: DateTime): Unit = {
    airDateTimeEpoch = dt.getMillis
  }

}
