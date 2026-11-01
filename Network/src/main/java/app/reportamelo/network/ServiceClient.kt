package app.reportamelo.network

import android.util.Log
import app.reportamelo.commons.Apis
import app.reportamelo.models.GenericResponse
import com.ensarsarajcic.kotlinx.serialization.msgpack.MsgPack
import com.ensarsarajcic.kotlinx.serialization.msgpack.MsgPackConfiguration
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer

data class NetworkDebugConfig(
    val enabled: Boolean = BuildConfig.ENABLE_NETWORK_LOGS,
    val logHeaders: Boolean = true,
    val logCookies: Boolean = true,
    val logRequestBody: Boolean = true,
    val logResponseBody: Boolean = true,
    val maxBodyLength: Int = 4000,
    val tag: String = "ServiceClient"
)

object NetworkDebugLogger {
    @Volatile
    var config: NetworkDebugConfig = NetworkDebugConfig()

    var isEnabled: Boolean
        get() = config.enabled
        set(value) {
            config = config.copy(enabled = value)
        }

    @PublishedApi
    internal var developmentModeOverride: Boolean? = null

    @PublishedApi
    internal var customLogWriter: ((priority: Int, tag: String, message: String, throwable: Throwable?) -> Unit)? = null

    val isDevelopment: Boolean
        get() = developmentModeOverride ?: (BuildConfig.DEBUG && app.reportamelo.commons.BuildConfig.DEBUG)

    @PublishedApi
    internal fun shouldLog(activeConfig: NetworkDebugConfig = config): Boolean {
        return isDevelopment && activeConfig.enabled
    }

    @PublishedApi
    internal fun logRequest(
        method: String,
        url: String,
        withOAuth: Boolean,
        requestBuilder: HttpRequestBuilder,
        bodyOverride: String? = null,
        activeConfig: NetworkDebugConfig = config
    ) {
        if (!shouldLog(activeConfig)) return

        val headerEntries = requestBuilder.headers.entries()
        val cookieHeader = requestBuilder.headers[HttpHeaders.Cookie]
        val contentTypeHeader = requestBuilder.headers[HttpHeaders.ContentType]

        val message = buildString {
            appendLine("┌──> HTTP $method $url")
            if (activeConfig.logCookies) {
                val cookieStatus = when {
                    !withOAuth && cookieHeader == null -> "OAuth: disabled (no Cookie)"
                    cookieHeader.isNullOrBlank() -> "OAuth: enabled | ⚠️ Cookie header missing"
                    cookieHeader == "session_id=" -> "OAuth: enabled | ⚠️ EMPTY SESSION COOKIE ($cookieHeader)"
                    else -> "OAuth: $withOAuth | Cookie: $cookieHeader"
                }
                appendLine("│ $cookieStatus")
            }
            if (activeConfig.logHeaders) {
                appendLine("│ Request Headers:")
                if (headerEntries.isEmpty()) {
                    appendLine("│   (none)")
                } else {
                    for ((key, values) in headerEntries) {
                        if (!activeConfig.logCookies && key.equals(HttpHeaders.Cookie, ignoreCase = true)) {
                            continue
                        }
                        appendLine("│   $key: ${values.joinToString("; ")}")
                    }
                }
            }
            if (activeConfig.logRequestBody) {
                val resolvedBody = bodyOverride ?: extractBodyDescription(requestBuilder.body, contentTypeHeader)
                if (resolvedBody != null) {
                    val payloadBytes = resolvedBody.toByteArray().size
                    appendLine("│ Request Payload ($payloadBytes bytes): ${truncate(resolvedBody, activeConfig.maxBodyLength)}")
                } else {
                    appendLine("│ Request Payload: (empty)")
                }
            }
            append("└──> END $method")
        }

        writeLog(Log.DEBUG, activeConfig.tag, message)
    }

    @PublishedApi
    internal fun resolveRequestPayload(
        requestBuilder: HttpRequestBuilder,
        bodyOverride: String? = null
    ): String? {
        val contentTypeHeader = requestBuilder.headers[HttpHeaders.ContentType]
        return bodyOverride ?: extractBodyDescription(requestBuilder.body, contentTypeHeader)
    }

    @PublishedApi
    internal fun logResponse(
        response: HttpResponse,
        bytes: ByteArray,
        durationMs: Long,
        decoderType: DecoderType,
        requestCookieFallback: String? = null,
        requestPayload: String? = null,
        activeConfig: NetworkDebugConfig = config
    ) {
        if (!shouldLog(activeConfig)) return

        val status = response.status
        val method = response.request.method.value
        val url = response.request.url.toString()
        val isSuccess = status.isSuccess()
        val sentCookie = runCatching { response.request.headers[HttpHeaders.Cookie] }.getOrNull()
            ?: requestCookieFallback
        val responseHeaders = response.headers.entries()

        val message = buildString {
            appendLine("┌<── HTTP ${status.value} ${status.description} $method $url (${durationMs}ms)")
            if (activeConfig.logCookies) {
                appendLine("│ Sent Cookie: ${sentCookie ?: "(none)"}")
                val setCookie = response.headers.getAll(HttpHeaders.SetCookie)
                if (!setCookie.isNullOrEmpty()) {
                    appendLine("│ Set-Cookie: ${setCookie.joinToString("; ")}")
                }
            }
            if (activeConfig.logRequestBody && !requestPayload.isNullOrEmpty()) {
                val payloadBytes = requestPayload.toByteArray().size
                appendLine("│ Request Payload ($payloadBytes bytes): ${truncate(requestPayload, activeConfig.maxBodyLength)}")
            }
            if (activeConfig.logHeaders) {
                appendLine("│ Response Headers:")
                if (responseHeaders.isEmpty()) {
                    appendLine("│   (none)")
                } else {
                    for ((key, values) in responseHeaders) {
                        appendLine("│   $key: ${values.joinToString("; ")}")
                    }
                }
            }
            if (activeConfig.logResponseBody) {
                val bodyPreview = when {
                    bytes.isEmpty() -> "(empty body, 0 bytes)"
                    decoderType == DecoderType.MESSAGE_PACK && isSuccess -> "(MessagePack binary body, ${bytes.size} bytes)"
                    else -> truncate(bytes.decodeToString(), activeConfig.maxBodyLength)
                }
                appendLine("│ Response Body (${bytes.size} bytes): $bodyPreview")
            }
            append("└<── END HTTP ${status.value}")
        }

        val priority = if (isSuccess) Log.DEBUG else Log.ERROR
        writeLog(priority, activeConfig.tag, message)
    }

    @PublishedApi
    internal fun logNetworkError(
        method: String,
        url: String,
        cookie: String?,
        durationMs: Long,
        error: Throwable,
        requestPayload: String? = null,
        activeConfig: NetworkDebugConfig = config
    ) {
        if (!shouldLog(activeConfig)) return

        val message = buildString {
            appendLine("┌<── HTTP FAILED $method $url (${durationMs}ms)")
            if (activeConfig.logCookies) {
                appendLine("│ Sent Cookie: ${cookie ?: "(none)"}")
            }
            if (activeConfig.logRequestBody && !requestPayload.isNullOrEmpty()) {
                val payloadBytes = requestPayload.toByteArray().size
                appendLine("│ Request Payload ($payloadBytes bytes): ${truncate(requestPayload, activeConfig.maxBodyLength)}")
            }
            appendLine("│ Error: ${error::class.simpleName}: ${error.message}")
            append("└<── END FAILED $method")
        }

        writeLog(Log.ERROR, activeConfig.tag, message, error)
    }

    @PublishedApi
    internal fun logDecodeError(
        targetType: String,
        rawText: String,
        error: Throwable,
        decoderType: DecoderType = DecoderType.JSON,
        activeConfig: NetworkDebugConfig = config
    ) {
        if (!shouldLog(activeConfig)) return
        val formatLabel = if (decoderType == DecoderType.MESSAGE_PACK) "MessagePack" else "JSON"
        val preview = truncate(rawText, activeConfig.maxBodyLength)
        writeLog(
            Log.ERROR,
            activeConfig.tag,
            "$formatLabel decode error for $targetType: ${error.message} | Body: $preview",
            error
        )
    }

    @PublishedApi
    internal fun formatMultipartSummary(
        fields: Map<String, String>,
        files: List<MultipartFormFile>
    ): String {
        val filesDescription = files.joinToString(", ") { file ->
            "{name=${file.name}, filename=${file.filename}, mimeType=${file.mimeType}, size=${file.data.size} bytes}"
        }
        return "multipart/form-data fields=$fields, files=[$filesDescription]"
    }

    private fun extractBodyDescription(body: Any, contentTypeHeader: String?): String? {
        return when (body) {
            is String -> body
            is TextContent -> body.text
            is OutgoingContent.ByteArrayContent -> {
                val bytes = body.bytes()
                val contentType = body.contentType?.toString() ?: contentTypeHeader.orEmpty()
                if (contentType.contains("json", ignoreCase = true) || contentType.startsWith("text/", ignoreCase = true)) {
                    bytes.decodeToString()
                } else {
                    "(${body.contentType ?: contentTypeHeader ?: body::class.simpleName}, contentLength=${bytes.size})"
                }
            }
            is OutgoingContent.NoContent -> null
            is OutgoingContent -> "(${body.contentType ?: contentTypeHeader ?: body::class.simpleName}, contentLength=${body.contentLength ?: "unknown"})"
            else -> {
                val className = body::class.simpleName ?: ""
                if (className == "EmptyContent" || className == "NullBody" || className == "Unit") null else body.toString()
            }
        }
    }

    private fun truncate(text: String, maxLength: Int): String {
        if (maxLength <= 0 || text.length <= maxLength) return text
        return text.take(maxLength) + "... (truncated, total ${text.length} chars)"
    }

    private fun writeLog(priority: Int, tag: String, message: String, throwable: Throwable? = null) {
        val writer = customLogWriter
        if (writer != null) {
            writer(priority, tag, message, throwable)
            return
        }
        runCatching {
            when (priority) {
                Log.ERROR -> if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
                Log.WARN -> if (throwable != null) Log.w(tag, message, throwable) else Log.w(tag, message)
                Log.INFO -> if (throwable != null) Log.i(tag, message, throwable) else Log.i(tag, message)
                else -> if (throwable != null) Log.d(tag, message, throwable) else Log.d(tag, message)
            }
        }
    }
}

class ServiceClient(
    @PublishedApi internal val baseURL: String, // E.g., Apis.apiV1
    @PublishedApi internal val decoderType: DecoderType = DecoderType.JSON,
    @PublishedApi internal val client: HttpClient = HttpClient(CIO),
    @PublishedApi internal var debugConfig: NetworkDebugConfig = NetworkDebugLogger.config
) {
    companion object {
        var debugEnabled: Boolean
            get() = NetworkDebugLogger.isEnabled
            set(value) {
                NetworkDebugLogger.isEnabled = value
            }

        var defaultDebugConfig: NetworkDebugConfig
            get() = NetworkDebugLogger.config
            set(value) {
                NetworkDebugLogger.config = value
            }
    }

    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @PublishedApi
    internal val msgPack = MsgPack(
        MsgPackConfiguration(
            ignoreUnknownKeys = true,
            strictTypes = false,
            preventOverflows = false
        )
    )

    @PublishedApi
    internal fun resolveDecoderType(response: HttpResponse): DecoderType {
        val contentType = response.headers[HttpHeaders.ContentType].orEmpty()
        return when {
            contentType.contains("msgpack", ignoreCase = true) -> DecoderType.MESSAGE_PACK
            contentType.contains("json", ignoreCase = true) -> DecoderType.JSON
            else -> decoderType
        }
    }
    
    @PublishedApi
    internal fun getOAuthHeader(): Pair<String, String> {
        val token = KeychainService.getToken(TokenKey.MUTATION)
        return "Cookie" to "session_id=$token"
    }

    @PublishedApi
    internal inline fun <reified Q : Any> buildQuery(query: Q?, requestBuilder: HttpRequestBuilder) {
        if (query == null) return
        val jsonElement = json.encodeToJsonElement(query)
        if (jsonElement is JsonObject) {
            for ((key, value) in jsonElement) {
                when (value) {
                    is JsonArray -> {
                        value.forEach { item ->
                            requestBuilder.parameter(key, item.jsonPrimitive.content)
                        }
                    }
                    is JsonPrimitive -> {
                        requestBuilder.parameter(key, value.content)
                    }
                    else -> Unit
                }
            }
        }
    }

    @PublishedApi
    internal inline fun <reified T> decodeResponse(
        bytes: ByteArray,
        effectiveDecoderType: DecoderType = decoderType
    ): T {
        return when (effectiveDecoderType) {
            DecoderType.JSON -> {
                val text = bytes.decodeToString()
                try {
                    json.decodeFromString<T>(text)
                } catch (e: Exception) {
                    NetworkDebugLogger.logDecodeError(
                        targetType = T::class.simpleName ?: "Unknown",
                        rawText = text,
                        error = e,
                        decoderType = DecoderType.JSON,
                        activeConfig = debugConfig
                    )
                    throw e
                }
            }
            DecoderType.MESSAGE_PACK -> {
                var unpackedJson: JsonElement? = null
                try {
                    val element = MsgPackJsonConverter.toJsonElement(bytes)
                    unpackedJson = element
                    json.decodeFromJsonElement<T>(element)
                } catch (primaryError: Exception) {
                    try {
                        msgPack.decodeFromByteArray(serializer<T>(), bytes)
                    } catch (_: Exception) {
                        NetworkDebugLogger.logDecodeError(
                            targetType = T::class.simpleName ?: "Unknown",
                            rawText = unpackedJson?.toString() ?: "(MessagePack binary, ${bytes.size} bytes)",
                            error = primaryError,
                            decoderType = DecoderType.MESSAGE_PACK,
                            activeConfig = debugConfig
                        )
                        throw primaryError
                    }
                }
            }
        }
    }

    @PublishedApi
    internal suspend fun handleError(response: HttpResponse, bytes: ByteArray) {
        if (response.status.isSuccess()) return

        val text = bytes.decodeToString()
        val genericResponse = try {
            json.decodeFromString<GenericResponse>(text)
        } catch (e: Exception) {
            GenericResponse(message = "No body", code = "NO_BODY")
        }

        when (response.status.value) {
            400 -> throw ServiceError.BadRequest(genericResponse)
            401 -> throw ServiceError.Unauthorized(genericResponse.code)
            403 -> throw ServiceError.Forbidden(genericResponse)
            404 -> throw ServiceError.NotFound
            405 -> throw ServiceError.NotAllowed
            406 -> throw ServiceError.NotAcceptable
            411 -> throw ServiceError.ContentLengthMissing
            415 -> throw ServiceError.UnsupportedMediaType
            422 -> throw ServiceError.Unprocessable
            429 -> throw ServiceError.TooManyRequests
            451 -> throw ServiceError.UnavailableForLegalReasons
            in 500..599 -> throw ServiceError.ServerError(genericResponse.code)
            else -> throw ServiceError.HttpStatus(response.status.value)
        }
    }

    @PublishedApi
    internal suspend inline fun <reified T> executeRequest(
        path: String,
        method: String,
        headers: Map<String, String> = emptyMap(),
        withOAuth: Boolean = false,
        requestPayload: String? = null,
        noinline builder: HttpRequestBuilder.() -> Unit
    ): T {
        val sanitizedPath = if (path.startsWith("/")) path.drop(1) else path
        val url = URLBuilder(baseURL).apply {
            appendPathSegments(sanitizedPath.split("/").filter { it.isNotEmpty() })
        }.build()

        val startTimeMs = System.currentTimeMillis()
        var loggedCookie: String? = null
        var loggedUrl: String = url.toString()
        var loggedPayload: String? = requestPayload

        val response = try {
            val requestBlock: HttpRequestBuilder.() -> Unit = {
                configure(headers, withOAuth, builder)
                loggedCookie = this.headers[HttpHeaders.Cookie]
                loggedUrl = this.url.buildString()
                loggedPayload = NetworkDebugLogger.resolveRequestPayload(this, requestPayload)
                NetworkDebugLogger.logRequest(
                    method = method,
                    url = loggedUrl,
                    withOAuth = withOAuth,
                    requestBuilder = this,
                    bodyOverride = loggedPayload,
                    activeConfig = debugConfig
                )
            }
            when (method) {
                "GET" -> client.get(url, requestBlock)
                "POST" -> client.post(url, requestBlock)
                "PUT" -> client.put(url, requestBlock)
                "DELETE" -> client.delete(url, requestBlock)
                "PATCH" -> client.patch(url, requestBlock)
                else -> throw IllegalArgumentException("Method $method not supported")
            }
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTimeMs
            NetworkDebugLogger.logNetworkError(
                method = method,
                url = loggedUrl,
                cookie = loggedCookie,
                durationMs = durationMs,
                error = e,
                requestPayload = loggedPayload,
                activeConfig = debugConfig
            )
            throw ServiceError.NetworkError(e)
        }

        val bytes = response.readBytes()
        val durationMs = System.currentTimeMillis() - startTimeMs
        val effectiveDecoderType = resolveDecoderType(response)
        NetworkDebugLogger.logResponse(
            response = response,
            bytes = bytes,
            durationMs = durationMs,
            decoderType = effectiveDecoderType,
            requestCookieFallback = loggedCookie,
            requestPayload = loggedPayload,
            activeConfig = debugConfig
        )
        handleError(response, bytes)
        
        if (response.status.value == 204 || bytes.isEmpty()) {
            return Unit as? T ?: throw ServiceError.InvalidResponse
        }

        return decodeResponse(bytes, effectiveDecoderType)
    }

    @PublishedApi
    internal fun HttpRequestBuilder.configure(
        headersMap: Map<String, String>,
        withOAuth: Boolean,
        builder: HttpRequestBuilder.() -> Unit
    ) {
        header("User-Agent", Apis.USER_AGENT)
        header("Accept-Language", "en-US")

        val blockedUsers = KeychainService.getArray("blockedUsers")
        if (blockedUsers.isNotEmpty()) {
            header("Blocked-Users", blockedUsers.joinToString(","))
        }

        if (withOAuth) {
            val (key, value) = getOAuthHeader()
            header(key, value)
        }

        if (!headersMap.containsKey(Apis.CLIENT_TYPE_HEADER)) {
            header(Apis.CLIENT_TYPE_HEADER, Apis.CLIENT_TYPE_ANDROID)
        }

        headersMap.forEach { (key, value) ->
            header(key, value)
        }
        
        builder()
    }

    suspend inline fun <reified T, reified Q : Any> gets(
        path: String,
        query: Q? = null,
        headers: Map<String, String> = emptyMap(),
        withOAuth: Boolean = false
    ): T {
        return executeRequest(path, "GET", headers, withOAuth) {
            contentType(ContentType.Application.Json)
            buildQuery(query, this)
        }
    }

    suspend inline fun <reified T> get(
        path: String,
        headers: Map<String, String> = emptyMap(),
        withOAuth: Boolean = false
    ): T {
        return gets<T, Unit>(path, null, headers, withOAuth)
    }

    suspend inline fun <reified V, reified T : Any> post(
        path: String,
        body: T,
        headers: Map<String, String> = emptyMap(),
        withOAuth: Boolean = false
    ): V {
        val payload = json.encodeToString(body)
        return executeRequest(path, "POST", headers, withOAuth, requestPayload = payload) {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
    }

    suspend inline fun <reified V, reified T : Any> put(
        path: String,
        body: T,
        headers: Map<String, String> = emptyMap(),
        withOAuth: Boolean = false
    ): V {
        val payload = json.encodeToString(body)
        return executeRequest(path, "PUT", headers, withOAuth, requestPayload = payload) {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
    }

    suspend inline fun <reified V, reified T : Any> delete(
        path: String,
        body: T,
        headers: Map<String, String> = emptyMap(),
        withOAuth: Boolean = false
    ): V {
        val payload = json.encodeToString(body)
        return executeRequest(path, "DELETE", headers, withOAuth, requestPayload = payload) {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
    }

    suspend inline fun <reified V, reified T : Any> patch(
        path: String,
        body: T,
        headers: Map<String, String> = emptyMap(),
        withOAuth: Boolean = false
    ): V {
        val payload = json.encodeToString(body)
        return executeRequest(path, "PATCH", headers, withOAuth, requestPayload = payload) {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
    }

    suspend inline fun <reified V> postMultipart(
        path: String,
        body: Map<String, String>,
        files: List<MultipartFormFile>,
        headers: Map<String, String> = emptyMap(),
        withOAuth: Boolean = false
    ): V {
        val sanitizedPath = if (path.startsWith("/")) path.drop(1) else path
        val url = URLBuilder(baseURL).apply {
            appendPathSegments(sanitizedPath.split("/").filter { it.isNotEmpty() })
        }.build()

        val startTimeMs = System.currentTimeMillis()
        var loggedCookie: String? = null
        var loggedUrl: String = url.toString()
        val multipartSummary = NetworkDebugLogger.formatMultipartSummary(body, files)

        val response = try {
            client.post(url) {
                configure(headers, withOAuth) {
                    setBody(
                        io.ktor.client.request.forms.MultiPartFormDataContent(
                            io.ktor.client.request.forms.formData {
                                body.forEach { (key, value) ->
                                    append(key, value)
                                }
                                files.forEach { file ->
                                    append(file.name, file.data, io.ktor.http.Headers.build {
                                        append(io.ktor.http.HttpHeaders.ContentType, file.mimeType)
                                        append(io.ktor.http.HttpHeaders.ContentDisposition, "filename=\"${file.filename}\"")
                                    })
                                }
                            }
                        )
                    )
                }
                loggedCookie = this.headers[HttpHeaders.Cookie]
                loggedUrl = this.url.buildString()
                NetworkDebugLogger.logRequest(
                    method = "POST",
                    url = loggedUrl,
                    withOAuth = withOAuth,
                    requestBuilder = this,
                    bodyOverride = multipartSummary,
                    activeConfig = debugConfig
                )
            }
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTimeMs
            NetworkDebugLogger.logNetworkError(
                method = "POST",
                url = loggedUrl,
                cookie = loggedCookie,
                durationMs = durationMs,
                error = e,
                requestPayload = multipartSummary,
                activeConfig = debugConfig
            )
            throw ServiceError.NetworkError(e)
        }
        
        val bytes = response.readBytes()
        val durationMs = System.currentTimeMillis() - startTimeMs
        val effectiveDecoderType = resolveDecoderType(response)
        NetworkDebugLogger.logResponse(
            response = response,
            bytes = bytes,
            durationMs = durationMs,
            decoderType = effectiveDecoderType,
            requestCookieFallback = loggedCookie,
            requestPayload = multipartSummary,
            activeConfig = debugConfig
        )
        handleError(response, bytes)
        
        if (response.status.value == 204 || bytes.isEmpty()) {
            return Unit as? V ?: throw ServiceError.InvalidResponse
        }

        return decodeResponse(bytes, effectiveDecoderType)
    }
}

@PublishedApi
internal object MsgPackJsonConverter {
    fun toJsonElement(bytes: ByteArray): JsonElement {
        if (bytes.isEmpty()) return JsonNull
        return Reader(bytes).readElement()
    }

    private class Reader(private val bytes: ByteArray) {
        private var pos = 0

        fun readElement(): JsonElement {
            val b = readUByte()
            return when {
                b in 0x00..0x7f -> JsonPrimitive(b)
                b in 0x80..0x8f -> readMap(b and 0x0f)
                b in 0x90..0x9f -> readArray(b and 0x0f)
                b in 0xa0..0xbf -> JsonPrimitive(readString(b and 0x1f))
                b in 0xe0..0xff -> JsonPrimitive(b.toByte().toInt())
                else -> when (b) {
                    0xc0, 0xc1 -> JsonNull
                    0xc2 -> JsonPrimitive(false)
                    0xc3 -> JsonPrimitive(true)
                    0xc4 -> JsonPrimitive(readBytes(readUByte()).decodeToString())
                    0xc5 -> JsonPrimitive(readBytes(readUInt16()).decodeToString())
                    0xc6 -> JsonPrimitive(readBytes(readInt32()).decodeToString())
                    0xc7 -> {
                        val len = readUByte()
                        skip(1 + len)
                        JsonNull
                    }
                    0xc8 -> {
                        val len = readUInt16()
                        skip(1 + len)
                        JsonNull
                    }
                    0xc9 -> {
                        val len = readInt32()
                        skip(1 + len)
                        JsonNull
                    }
                    0xca -> JsonPrimitive(Float.fromBits(readInt32()))
                    0xcb -> JsonPrimitive(Double.fromBits(readInt64()))
                    0xcc -> JsonPrimitive(readUByte())
                    0xcd -> JsonPrimitive(readUInt16())
                    0xce -> JsonPrimitive(readUInt32())
                    0xcf -> JsonPrimitive(readInt64())
                    0xd0 -> JsonPrimitive(readByte().toInt())
                    0xd1 -> JsonPrimitive(readInt16().toInt())
                    0xd2 -> JsonPrimitive(readInt32())
                    0xd3 -> JsonPrimitive(readInt64())
                    0xd4 -> { skip(2); JsonNull }
                    0xd5 -> { skip(3); JsonNull }
                    0xd6 -> { skip(5); JsonNull }
                    0xd7 -> { skip(9); JsonNull }
                    0xd8 -> { skip(17); JsonNull }
                    0xd9 -> JsonPrimitive(readString(readUByte()))
                    0xda -> JsonPrimitive(readString(readUInt16()))
                    0xdb -> JsonPrimitive(readString(readInt32()))
                    0xdc -> readArray(readUInt16())
                    0xdd -> readArray(readInt32())
                    0xde -> readMap(readUInt16())
                    0xdf -> readMap(readInt32())
                    else -> throw IllegalArgumentException("Unsupported MessagePack byte: 0x${b.toString(16)}")
                }
            }
        }

        private fun readArray(size: Int): JsonArray {
            if (size < 0) throw IllegalArgumentException("Negative MessagePack array size: $size")
            val list = ArrayList<JsonElement>(size)
            repeat(size) {
                list.add(readElement())
            }
            return JsonArray(list)
        }

        private fun readMap(size: Int): JsonObject {
            if (size < 0) throw IllegalArgumentException("Negative MessagePack map size: $size")
            val map = LinkedHashMap<String, JsonElement>(size)
            repeat(size) {
                val keyElement = readElement()
                val key = (keyElement as? JsonPrimitive)?.content ?: keyElement.toString()
                val value = readElement()
                map[key] = value
            }
            return JsonObject(map)
        }

        private fun readString(length: Int): String {
            if (length == 0) return ""
            if (length < 0 || pos + length > bytes.size) {
                throw IllegalArgumentException("Invalid MessagePack string length $length at offset $pos (size=${bytes.size})")
            }
            val str = bytes.decodeToString(pos, pos + length)
            pos += length
            return str
        }

        private fun readBytes(length: Int): ByteArray {
            if (length == 0) return ByteArray(0)
            if (length < 0 || pos + length > bytes.size) {
                throw IllegalArgumentException("Invalid MessagePack bin length $length at offset $pos (size=${bytes.size})")
            }
            val slice = bytes.copyOfRange(pos, pos + length)
            pos += length
            return slice
        }

        private fun skip(count: Int) {
            if (count < 0 || pos + count > bytes.size) {
                throw IllegalArgumentException("Invalid MessagePack skip $count at offset $pos (size=${bytes.size})")
            }
            pos += count
        }

        private fun readByte(): Byte {
            if (pos >= bytes.size) throw IllegalArgumentException("Unexpected EOF in MessagePack at offset $pos")
            return bytes[pos++]
        }

        private fun readUByte(): Int = readByte().toInt() and 0xff

        private fun readInt16(): Short {
            val b0 = readUByte()
            val b1 = readUByte()
            return ((b0 shl 8) or b1).toShort()
        }

        private fun readUInt16(): Int {
            val b0 = readUByte()
            val b1 = readUByte()
            return (b0 shl 8) or b1
        }

        private fun readInt32(): Int {
            val b0 = readUByte()
            val b1 = readUByte()
            val b2 = readUByte()
            val b3 = readUByte()
            return (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
        }

        private fun readUInt32(): Long = readInt32().toLong() and 0xffffffffL

        private fun readInt64(): Long {
            val hi = readInt32().toLong() and 0xffffffffL
            val lo = readInt32().toLong() and 0xffffffffL
            return (hi shl 32) or lo
        }
    }
}

class MultipartFormFile(
    val name: String,
    val filename: String,
    val mimeType: String,
    val data: ByteArray
)
