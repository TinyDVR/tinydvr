package org.tinydvr.service

import org.joda.time._
import org.tinydvr.schedulesdirect.api.ProgramResponse

/**
 * The types served by the core api for tinydvr.
 */

case class ScheduledProgram(program: ProgramResponse,
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