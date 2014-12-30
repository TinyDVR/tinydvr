package org.tinydvr.config

abstract class Configuration {

  // The database connection information
  def databaseInfo: DatabaseConnectionInfo

  // Authentication credentials for schedules direct.
  def schedulesDirectCredentials: SchedulesDirectCredentials
}
