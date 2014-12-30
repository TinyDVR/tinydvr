package org.tinydvr.service.api

import org.joda.time._

/**
 * The types served by the core api for tinydvr.
 * Many of these are also used as DTOs or serialized in the database.
 */

case class Program(programID: String, // the program id provided by schedules direct
                   title: String,
                   castInfo: List[ProgramPerson],
                   description: Option[String],
                   episodeTitle: Option[String],
                   episode: Option[Int],
                   genres: List[String],
                   movieInfo: Option[MovieInfo],
                   originalAirDate: Option[LocalDate],
                   season: Option[Int],
                   showType: Option[String])

case class ScheduledProgram(program: Program,
                            stationID: String, // the station ID provided by schedules direct.
                            start: DateTimeDTO,
                            end: DateTimeDTO,
                            durationInSeconds: Int)

case class DateTimeDTO(epoch: Long,
                       dateTime: DateTime,
                       date: LocalDate,
                       time: LocalTime)

case class ProgramPerson(personId: String, // id provided by schedules direct
                         name: String,
                         role: String,
                         characterName: Option[String],
                         billingOrder: Int)

case class MovieInfo(year: Int,
                     duration: Option[Int])