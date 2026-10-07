package app.reportamelo.commons.utils

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration

class TimeoutError : Exception("timedOut")

suspend fun <T> withTimeoutAction(duration: Duration, operation: suspend () -> T): T {
    return try {
        withTimeout(duration) {
            operation()
        }
    } catch (e: TimeoutCancellationException) {
        throw TimeoutError()
    }
}
