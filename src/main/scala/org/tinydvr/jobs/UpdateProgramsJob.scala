package org.tinydvr.jobs

import org.joda.time.DateTime
import org.tinydvr.config.TinyDvrConfiguration
import org.tinydvr.db.Program
import org.tinydvr.util.SchedulesDirectAPI

case class UpdateProgramsJob(tinyDvrConfiguration: TinyDvrConfiguration) extends BaseJob with SchedulesDirectAPI {

  import org.tinydvr.schedulesdirect.api.Implicits._

  def runInternal(): Unit = {

    // figure out which programs we need to download data for
    val scheduledProgramIdByMd5 =
      tinyDvrDb.findAllSchedules.groupBy(_.programId).
      mapValues(_.maxBy(_.airDateTime.getMillis).programMd5).
      map(_.swap)
    val cachedMd5s = tinyDvrDb.findProgramsByMd5s(scheduledProgramIdByMd5.keySet).map(_.md5).toSet
    val programsToFecth = scheduledProgramIdByMd5.filterKeys(md5 => !cachedMd5s.contains(md5)).values.toSet

    // fetch the new program information
    logger.info(s"Fetching ${programsToFecth.size} programs.")
    val programInfo = schedulesDirectAPI.getPrograms(programsToFecth).getResult

    // update the database
    val now = System.currentTimeMillis
    val programs = programInfo.map(program => {
      val p = new Program
      p.id = program.programID
      p.lastUpdated = now
      p.md5 = program.md5
      p.searchableTitle = program.titles.head.title120.toLowerCase
      p.program = program
      p
    })
    logger.info(s"Updating ${programs.size} programs in the database.")
    tinyDvrDb.insertOrReplaceProgram(programs)

    // purge old programs
    val purgeDate = new DateTime().minusDays(tinyDvrConfiguration.listings.retainProgramsPeriodInDays)
    logger.info(s"Erasing programs not seen since ${purgeDate}")
    val numDeleted = tinyDvrDb.eraseProgramsNotSeenSince(purgeDate)
    logger.info(s"Erased ${numDeleted} programs.")
    tinyDvrConfiguration.lastListingsUpdate = new DateTime
  }
}
