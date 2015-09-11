package org.tinydvr.util

import org.tinydvr.db._

trait VariableReplacer {

  lazy val variableReplacer = VariableReplacer

}

object VariableReplacer {

  def replace(strings: Iterable[String], s: Station, p: Program, r: Recording): Iterable[String] = {
    val doReplace = getVaraiblesMap(s, p, r).map {
      case (k, v) => (s: String) => s.replaceAll(k, v)
    }.reduceLeft(_ andThen _)
    strings.map(doReplace)
  }

  def replace(string: String, s: Station, p: Program, r: Recording): String = {
    getVaraiblesMap(s, p, r).map {
      case (k, v) => (s: String) => s.replaceAll(k, v)
    }.reduceLeft(_ andThen _)(string)
  }

  private val stationVariablesMap = Map(
    "%callsign" -> ((s: Station) => s.info.callsign),
    "%atscMajor" -> ((s: Station) => s.atscMajor.toString),
    "%atscMinor" -> ((s: Station) => s.atscMinor.toString)
  )

  private val programVariablesMap = Map(
    "%season" -> ((p: Program) => p.program.metadata.flatMap(_.values.map(_.season)).headOption.getOrElse("")),
    "%episode" -> ((p: Program) => p.program.metadata.flatMap(_.values.flatMap(_.episode)).headOption.getOrElse(""))
  )

  private val recordingsVariablesMap = Map(
    "%title" -> ((r: Recording) => r.programTitle),
    "%duration" -> ((r: Recording) => r.durationInSeconds.toString)
  )

  private def getVaraiblesMap(s: Station, p: Program, r: Recording): Map[String, String] = {
    stationVariablesMap.mapValues(_.apply(s)) ++
      programVariablesMap.mapValues(_.apply(p)) ++
      recordingsVariablesMap.mapValues(_.apply(r))
  }

}
