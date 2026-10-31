package io.github.ziomekmateusz.iso100.calc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.max

fun assertClose(expected: Double, actual: Double, what: Any, relative: Double = 1e-9) {
    if (abs(expected - actual) > relative * max(abs(expected), abs(actual))) fail<Unit>("$what: expected $expected, got $actual")
}

/** ±2 minutes, the tolerance for sun and moon times. */
fun assertTimeClose(expected: String?, actual: ZonedDateTime?, what: Any, tolerance: Duration = Duration.ofMinutes(2)) {
    if (expected == null || actual == null) {
        assertEquals(expected, actual?.toOffsetDateTime()?.toString(), "$what")
        return
    }
    val diff = Duration.between(OffsetDateTime.parse(expected).toInstant(), actual.toInstant()).abs()
    if (diff > tolerance) fail<Unit>("$what: expected $expected, got $actual")
}

fun <T> Outcome<T>.ok(): T = (this as? Outcome.Ok)?.value ?: fail("expected a value, got $this")
