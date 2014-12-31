package org.tinydvr.service.jobs

import org.joda.time.DateTime
import scala.concurrent.Future

case class UpdateLineupsJob() extends BaseJob {
  import org.tinydvr.schedulesdirect.api.Implicits._
  private val LINEUP_EXPIRE_DAYS = 14

  def run(): Unit = {
    // get what lineups are currently in the database
    val currentLineups = tinyDvrDb.getAllLineups()

    // download the lineups for the users account
    val sdApi = schedulesDirectApi()
    val sdLineups = sdApi.getLineups.getResult
    val sdUris = sdLineups.lineups.map(_.uri).toSet

    // remove any lineups no longer associate with the user
    val lineupsToRemove = currentLineups.filterNot(l => sdUris.contains(l.uri))
    lineupsToRemove.foreach(l => {
      logger.info("Removing Lineup %s as it is no longer associated with the user.".format(l.uri))
      tinyDvrDb.eraseLineup(l.id)
    })

    // Add or update any lineups from the server
    val expiredDate = new DateTime().minusDays(LINEUP_EXPIRE_DAYS)
    val notExpiredUris = currentLineups.filter(_.lastUpdated.isAfter(expiredDate)).map(_.uri).toSet
    val urisToUpdate = sdUris -- notExpiredUris
    val updatedLineups = Future.sequence(urisToUpdate.map(uri => {
      sdApi.getLineup(uri).map((uri, _))
    })).getResult
    updatedLineups.foreach {
      case (uri, result) => tinyDvrDb.insertOrUpdateLineup(uri, result)
    }
    logger.info("Updated data for %d lineups".format(updatedLineups.size))
  }
}
