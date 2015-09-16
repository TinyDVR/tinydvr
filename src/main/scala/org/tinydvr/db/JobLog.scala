package org.tinydvr.db

import org.squeryl.KeyedEntity
import org.squeryl.annotations._

class JobLog extends KeyedEntity[Long]{

  @Column(name = "id")  // auto-incremented
  var id: Long = _

  @Column(name = "name") // The name of the job
  var name: String = _

  @Column(name = "timestamp") // the last time the job was run
  var timestamp: Long = _

  @Column(name = "status")
  var status:JobStatus.Value = _
}

object JobStatus extends Enumeration {
    type JobStatus = Value

    val Successful = Value(0, "Successful")
    val Failed = Value(1, "Failed")
}
