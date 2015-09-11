package org.tinydvr.config

/**
 * A class storing static configuration values (i.e. ones that will
 * not change during the life of the process).
 */
case class StaticConfiguration(
  // The database connection information
  databaseInfo: DatabaseConnectionInfo,
  // Authentication credentials for schedules direct.
  schedulesDirectCredentials: SchedulesDirectCredentials,
  // listings configuration
  listings: SchedulingListingConfiguration,
  // update freuquencies
  updateFrequencies: SchedulingUpdateFrequenciesConfiguration,
  // the recording configuration
  recordings: RecordingConfig,
  // the tuner configuration
  tuner: TunerConfig
)
