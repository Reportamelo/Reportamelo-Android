package app.reportamelo.network

import app.reportamelo.models.GenericResponse
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
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import com.ensarsarajcic.kotlinx.serialization.msgpack.MsgPack

class ServiceClient(
    @PublishedApi internal val baseURL: String, // E.g., Apis.apiV1
    @PublishedApi internal val decoderType: DecoderType = DecoderType.JSON
) {
    @PublishedApi
    internal val client = HttpClient(CIO)
    @PublishedApi
    internal val json = Json { ignoreUnknownKeys = true; isLenient = true }
    
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
    internal suspend inline fun <reified T> decodeResponse(response: HttpResponse): T {
        val bytes = response.readBytes()
        return when (decoderType) {
            DecoderType.JSON -> {
                val text = bytes.decodeToString()
                json.decodeFromString<T>(text)
            }
            DecoderType.MESSAGE_PACK -> {
                MsgPack.decodeFromByteArray(serializer<T>(), bytes) as T
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
        noinline builder: HttpRequestBuilder.() -> Unit
    ): T {
        val sanitizedPath = if (path.startsWith("/")) path.drop(1) else path
        val url = URLBuilder(baseURL).apply {
            appendPathSegments(sanitizedPath.split("/").filter { it.isNotEmpty() })
        }.build()

        val response = try {
            when (method) {
                "GET" -> client.get(url) { configure(headers, withOAuth, builder) }
                "POST" -> client.post(url) { configure(headers, withOAuth, builder) }
                "PUT" -> client.put(url) { configure(headers, withOAuth, builder) }
                "DELETE" -> client.delete(url) { configure(headers, withOAuth, builder) }
                "PATCH" -> client.patch(url) { configure(headers, withOAuth, builder) }
                else -> throw IllegalArgumentException("Method $method not supported")
            }
        } catch (e: Exception) {
            throw ServiceError.NetworkError(e)
        }

        val bytes = response.readBytes()
        handleError(response, bytes)
        
        if (response.status.value == 204 || bytes.isEmpty()) {
            return Unit as? T ?: throw ServiceError.InvalidResponse
        }

        return decodeResponse(response)
    }

    @PublishedApi
    internal fun HttpRequestBuilder.configure(
        headersMap: Map<String, String>,
        withOAuth: Boolean,
        builder: HttpRequestBuilder.() -> Unit
    ) {
        header("User-Agent", "Reportamelo/1.0")
        header("Accept-Language", "en-US")

        val blockedUsers = KeychainService.getArray("blockedUsers")
        if (blockedUsers.isNotEmpty()) {
            header("Blocked-Users", blockedUsers.joinToString(","))
        }

        if (withOAuth) {
            val (key, value) = getOAuthHeader()
            header(key, value)
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
        return executeRequest(path, "POST", headers, withOAuth) {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(body))
        }
    }

    suspend inline fun <reified V, reified T : Any> put(
        path: String,
        body: T,
        headers: Map<String, String> = emptyMap(),
        withOAuth: Boolean = false
    ): V {
        return executeRequest(path, "PUT", headers, withOAuth) {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(body))
        }
    }

    suspend inline fun <reified V, reified T : Any> delete(
        path: String,
        body: T,
        headers: Map<String, String> = emptyMap(),
        withOAuth: Boolean = false
    ): V {
        return executeRequest(path, "DELETE", headers, withOAuth) {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(body))
        }
    }

    suspend inline fun <reified V, reified T : Any> patch(
        path: String,
        body: T,
        headers: Map<String, String> = emptyMap(),
        withOAuth: Boolean = false
    ): V {
        return executeRequest(path, "PATCH", headers, withOAuth) {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(body))
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
            }
        } catch (e: Exception) {
            throw ServiceError.NetworkError(e)
        }
        
        val bytes = response.readBytes()
        handleError(response, bytes)
        
        if (response.status.value == 204 || bytes.isEmpty()) {
            return Unit as? V ?: throw ServiceError.InvalidResponse
        }

        return decodeResponse(response)
    }
}

class MultipartFormFile(
    val name: String,
    val filename: String,
    val mimeType: String,
    val data: ByteArray
)
