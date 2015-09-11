package org.tinydvr.util

import org.tinydvr.config.StaticConfiguration

/**
 * A class implementing the 'behavior logic' of recordings.
 */
class RecordingsAPI(val staticConfiguration: StaticConfiguration) {

  /**
   * Indicates that the specific recording has started.
   * Two things happen here:
   *  1) The status is changed to 'in progress'
   *  2) The output file is populated.
   */
  def beginRecording(recordingId: Long, file: String): Unit = {}
}
