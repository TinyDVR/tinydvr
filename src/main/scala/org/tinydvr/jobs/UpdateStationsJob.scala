package org.tinydvr.jobs

import org.joda.time.DateTime
import org.tinydvr.config.TinyDvrConfiguration
import org.tinydvr.db._
import org.tinydvr.util.SchedulesDirectAPI

case class UpdateStationsJob(tinyDvrConfiguration: TinyDvrConfiguration) extends BaseJob with SchedulesDirectAPI {

  import org.tinydvr.schedulesdirect.api.Implicits._

  def runInternal(): Unit = {

    // get the user's lineup
    val lineup = schedulesDirectAPI.getLineups.getResult.lineups.head
    logger.info(s"Updating stations for lineup ${lineup.name}")

    //
    // Update Stations info
    //

    // get the station mapping for the lineup
    val lineupMapping = schedulesDirectAPI.getLineup(lineup.uri).getResult

    // update the stations
    val infoByStationId = lineupMapping.stations.map(s => (s.stationID, s)).toMap
    val stations = for {
      channel <- lineupMapping.map
      atscMajor <- channel.atscMajor
      atscMinor <- channel.atscMinor
      info <- infoByStationId.get(channel.stationID)
    } yield {
      val s = new Station
      s.id = channel.stationID
      s.uhfVhf = channel.uhfVhf
      s.atscMajor = atscMajor
      s.atscMinor = atscMinor
      s.info = info
      s
    }
    logger.info(s"Updating ${stations.size} stations for lineup ${lineup.name}")
    tinyDvrDb.replaceAllStations(stations)
    tinyDvrConfiguration.lastStationUpdate = new DateTime
  }
}
