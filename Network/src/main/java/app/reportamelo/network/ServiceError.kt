package app.reportamelo.network

import app.reportamelo.models.GenericResponse

sealed class ServiceError(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    class NetworkError(cause: Throwable) : ServiceError(cause.message, cause)
    object BaseURLMissing : ServiceError("Base URL missing")
    object InvalidResponse : ServiceError("Invalid response")
    class HttpStatus(val statusCode: Int) : ServiceError("HTTP Status $statusCode")
    class BadRequest(val response: GenericResponse) : ServiceError(response.message)
    class Unauthorized(val code: String) : ServiceError("Unauthorized: $code")
    class Forbidden(val response: GenericResponse) : ServiceError("Forbidden: ${response.message}")
    object NotFound : ServiceError("Not Found")
    object NotAllowed : ServiceError("Method Not Allowed")
    object NotAcceptable : ServiceError("Not Acceptable")
    object ContentLengthMissing : ServiceError("Content Length Missing")
    object UnsupportedMediaType : ServiceError("Unsupported Media Type")
    object UnavailableForLegalReasons : ServiceError("Unavailable For Legal Reasons")
    class ServerError(val code: String) : ServiceError("Server Error: $code")
    object TooManyRequests : ServiceError("Too Many Requests")
    object Unprocessable : ServiceError("Unprocessable Entity")
}
