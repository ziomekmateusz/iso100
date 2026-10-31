package io.github.ziomekmateusz.iso100.calc.astro

import java.time.ZoneId
import java.time.ZonedDateTime

/** A place on Earth and the time zone used to show its times. */
data class Place(val latitude: Double, val longitude: Double, val zone: ZoneId)

/** When a body crosses an altitude during a day; [rise] and [set] are null when that does not happen, as with the midnight sun. */
data class Crossing(val rise: ZonedDateTime?, val set: ZonedDateTime?, val alwaysAbove: Boolean, val alwaysBelow: Boolean)

/** A span of time: [start] is null if it began the day before; [end] may fall on the next day and is null if it does not end within a day. */
data class TimeRange(val start: ZonedDateTime?, val end: ZonedDateTime?)

/** Altitude above the horizon and azimuth from north towards east, in degrees. */
data class SkyPosition(val altitudeDeg: Double, val azimuthDeg: Double)
