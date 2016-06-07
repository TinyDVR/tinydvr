package org.tinydvr.service

import org.joda.time._
import org.tinydvr.schedulesdirect.api.{LineupStationInfo, ProgramResponse}

/**
 * The types served by the core api for tinydvr.
 */

case class ScheduledProgramsResponse(station: StationDTO, programs: List[ScheduledProgram])

case class StationDTO(id: String,
                      info: LineupStationInfo,
                      tuning: AntennaTuningInfo)

case class AntennaTuningInfo(uhfVhf: Int,
                             atscMajor: Int,
                             atscMinor: Int)

case class ScheduledProgram(airDateTime: DateTimeDTO,
                            programID: String,
                            program: ProgramResponse,
                            stationID: String, // the station ID provided by schedules direct.
                            durationInSeconds: Int,
                            willBeRecorded: Boolean)

case class DateTimeDTO(epoch: Long,
                       dateTime: DateTime,
                       date: LocalDate,
                       time: LocalTime)