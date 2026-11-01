package app.reportamelo.network

import android.util.Log
import app.reportamelo.commons.AppPreferences
import app.reportamelo.commons.AttachmentType
import app.reportamelo.commons.AttachmentValidatedBy
import app.reportamelo.commons.DeviceType
import app.reportamelo.commons.ReportAttachmentState
import app.reportamelo.models.AttachMediaResponse
import app.reportamelo.models.CustomizedResponse
import app.reportamelo.models.DeviceTokenRequest
import app.reportamelo.models.GenericResponse
import app.reportamelo.models.MapExplorerQueryParams
import app.reportamelo.models.MapExplorerReport
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer

class ServiceClientDebugLoggerTest {

    data class CapturedLog(
        val priority: Int,
        val tag: String,
        val message: String,
        val throwable: Throwable?
    )

    private val capturedLogs = mutableListOf<CapturedLog>()
    private var server: HttpServer? = null
    private var baseUrl: String = ""

    @Before
    fun setUp() {
        capturedLogs.clear()
        AppPreferences.clearAll()
        NetworkDebugLogger.developmentModeOverride = true
        NetworkDebugLogger.config = NetworkDebugConfig(enabled = true)
        NetworkDebugLogger.customLogWriter = { priority, tag, message, throwable ->
            capturedLogs.add(CapturedLog(priority, tag, message, throwable))
        }

        val httpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        httpServer.createContext("/v1/attach-media/upload") { exchange ->
            val responseJson = """{"id":"","message":"Forbidden, requires one of roles: user, moderator","code":"FORBIDDEN_ROLE"}"""
            val bytes = responseJson.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(403, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        httpServer.createContext("/v1/reports/start") { exchange ->
            val responseJson = """{"id":"1","message":"OK","code":"SUCCESS"}"""
            val bytes = responseJson.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        httpServer.createContext("/v1/user/device/token") { exchange ->
            val responseJson = """{"id":"DEVICE_TOKEN_UPDATED","message":"","code":"Device token updated successfully."}"""
            val bytes = responseJson.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        httpServer.createContext("/v1/map-explorer/reports") { exchange ->
            val bytes = buildSampleMapExplorerMsgPackPayload()
            exchange.responseHeaders.add("Content-Type", "application/x-msgpack")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        httpServer.start()
        server = httpServer
        baseUrl = "http://127.0.0.1:${httpServer.address.port}/v1"
    }

    @After
    fun tearDown() {
        server?.stop(0)
        server = null
        NetworkDebugLogger.customLogWriter = null
        NetworkDebugLogger.developmentModeOverride = null
        NetworkDebugLogger.config = NetworkDebugConfig()
        AppPreferences.clearAll()
    }

    @Test
    fun `postMultipart 403 logs request and response with session_id cookie in development`() = runBlocking {
        KeychainService.setToken(TokenKey.MUTATION, "guest-session-abc-123")
        val client = ServiceClient(baseURL = baseUrl)

        try {
            client.postMultipart<CustomizedResponse<AttachMediaResponse>>(
                path = "attach-media/upload",
                body = emptyMap(),
                files = listOf(
                    MultipartFormFile(
                        name = "picture",
                        filename = "report-picture.webp",
                        mimeType = "image/webp",
                        data = ByteArray(128) { 1 }
                    )
                ),
                headers = mapOf(
                    "report-container" to "container-001",
                    "upload-mode" to "one-by-one"
                ),
                withOAuth = true
            )
            fail("Expected ServiceError.Forbidden to be thrown")
        } catch (e: ServiceError.Forbidden) {
            assertEquals("FORBIDDEN_ROLE", e.response.code)
        }

        assertEquals(2, capturedLogs.size)
        val requestLog = capturedLogs[0]
        val responseLog = capturedLogs[1]

        assertEquals(Log.DEBUG, requestLog.priority)
        assertTrue(requestLog.message.contains("HTTP POST $baseUrl/attach-media/upload"))
        assertTrue(requestLog.message.contains("Cookie: session_id=guest-session-abc-123"))
        assertTrue(requestLog.message.contains("report-container: container-001"))
        assertTrue(requestLog.message.contains("upload-mode: one-by-one"))
        assertTrue(requestLog.message.contains("filename=report-picture.webp, mimeType=image/webp, size=128 bytes"))

        assertEquals(Log.ERROR, responseLog.priority)
        assertTrue(responseLog.message.contains("HTTP 403"))
        assertTrue(responseLog.message.contains("Sent Cookie: session_id=guest-session-abc-123"))
        assertTrue(responseLog.message.contains("FORBIDDEN_ROLE"))
    }

    @Test
    fun `warns when OAuth is enabled but session token is empty`() = runBlocking {
        KeychainService.deleteToken(TokenKey.MUTATION)
        val client = ServiceClient(baseURL = baseUrl)

        val response = client.get<GenericResponse>(
            path = "reports/start",
            withOAuth = true
        )

        assertEquals("SUCCESS", response.code)
        assertEquals(2, capturedLogs.size)
        assertTrue(capturedLogs[0].message.contains("EMPTY SESSION COOKIE (session_id=)"))
        assertTrue(capturedLogs[1].message.contains("Sent Cookie: session_id="))
    }

    @Test
    fun `does not log when disabled via config`() = runBlocking {
        KeychainService.setToken(TokenKey.MUTATION, "session-xyz")
        ServiceClient.debugEnabled = false
        val client = ServiceClient(baseURL = baseUrl)

        client.get<GenericResponse>(
            path = "reports/start",
            withOAuth = true
        )

        assertTrue("Expected no logs when debugEnabled is false", capturedLogs.isEmpty())
    }

    @Test
    fun `does not log when development mode is false even if config is enabled`() = runBlocking {
        KeychainService.setToken(TokenKey.MUTATION, "session-xyz")
        ServiceClient.debugEnabled = true
        NetworkDebugLogger.developmentModeOverride = false
        val client = ServiceClient(baseURL = baseUrl)

        client.get<GenericResponse>(
            path = "reports/start",
            withOAuth = true
        )

        assertFalse(NetworkDebugLogger.shouldLog())
        assertTrue("Expected no logs outside development mode", capturedLogs.isEmpty())
    }

    @Test
    fun `patch logs Request Payload in both request and response blocks`() = runBlocking {
        KeychainService.setToken(TokenKey.MUTATION, "session-token-xyz")
        val client = ServiceClient(baseURL = baseUrl)
        val payload = DeviceTokenRequest(
            deviceToken = "0fff3cfb8d4ad5f8",
            deviceId = "0fff3cfb8d4ad5f8",
            platform = DeviceType.ANDROID
        )

        val response = client.patch<GenericResponse, DeviceTokenRequest>(
            path = "user/device/token",
            body = payload,
            withOAuth = true
        )

        assertEquals("DEVICE_TOKEN_UPDATED", response.id)
        assertEquals(2, capturedLogs.size)

        val expectedJson = """{"deviceToken":"0fff3cfb8d4ad5f8","deviceId":"0fff3cfb8d4ad5f8","platform":"android","isActive":true}"""
        val requestLog = capturedLogs[0]
        val responseLog = capturedLogs[1]

        assertTrue("Actual request log:\n${requestLog.message}", requestLog.message.contains("Request Payload (${expectedJson.toByteArray().size} bytes): $expectedJson"))
        assertTrue("Actual response log:\n${responseLog.message}", responseLog.message.contains("Request Payload (${expectedJson.toByteArray().size} bytes): $expectedJson"))
    }

    @Test
    fun `MapExplorerService decodes application x-msgpack response with omitted optional fields and extra keys`() = runBlocking {
        val service = MapExplorerService(
            client = ServiceClient(
                baseURL = baseUrl,
                decoderType = DecoderType.MESSAGE_PACK
            )
        )

        val reports = service.reports(
            MapExplorerQueryParams(
                lat = 13.7269865,
                lng = -89.1809165,
                radius = 600,
                issueTypeIds = listOf(1),
                severityIds = listOf(1),
                statusIds = emptyList()
            )
        )

        assertEquals(2, reports.size)
        val first = reports[0]
        assertEquals("SV-SSC-20260907-Dhly4eZs2nVGJ2gY", first.id)
        assertEquals(13.7269865, first.lat, 0.000001)
        assertEquals(-89.1809165, first.lng, 0.000001)
        assertEquals("Area Urbana, Cuscatancingo, San Salvador, El Salvador", first.address)
        assertEquals("Baches", first.title)
        assertEquals("65GPRHC+QC", first.ocl)
        assertEquals("Baches", first.description)
        assertEquals(1, first.severityId)
        assertEquals(2, first.statusId)
        assertEquals("/Dhly4eZs2nVGJ2gY/report/20260907/SSC/SV/baches", first.shareUrl)
        assertEquals("d8dcd05d-71be-45b3-bd65-d50af060c5da", first.reportContainer)
        assertEquals("1eEWAXwkcmYnUog", first.profileId)
        assertEquals("Las imágenes muestran claramente los baches reportados.", first.observations)
        assertEquals(1, first.attachments.size)

        val attachment = first.attachments[0]
        assertEquals("01a07aee-8a62-7661-abd0-bc064132f7dd", attachment.id)
        assertEquals(AttachmentType.IMAGE, attachment.type)
        assertEquals(AttachmentValidatedBy.BOT, attachment.validatedBy)
        assertEquals(ReportAttachmentState.CONFIRMED, attachment.state)
        assertEquals("Imagen muestra claramente los baches en la carretera.", attachment.notes)
        assertEquals(1788768859734L, attachment.validatedAtRaw)

        val second = reports[1]
        assertEquals("SV-SSC-20260906-40nPehqkecslz7GJ", second.id)
        assertEquals(1788768000000L, second.reportedAtRaw)

        val responseLog = capturedLogs.last()
        assertTrue(
            "Expected MessagePack binary summary in response log, got:\n${responseLog.message}",
            responseLog.message.contains("(MessagePack binary body,")
        )
    }

    @Test
    fun `ServiceClient auto-detects application x-msgpack Content-Type even when configured with JSON`() = runBlocking {
        val client = ServiceClient(baseURL = baseUrl, decoderType = DecoderType.JSON)

        val reports = client.gets<List<MapExplorerReport>, MapExplorerQueryParams>(
            path = "map-explorer/reports",
            query = MapExplorerQueryParams(
                lat = 13.7269865,
                lng = -89.1809165,
                radius = 600,
                issueTypeIds = listOf(1),
                severityIds = listOf(1),
                statusIds = emptyList()
            )
        )

        assertEquals(2, reports.size)
        assertEquals("SV-SSC-20260907-Dhly4eZs2nVGJ2gY", reports[0].id)
    }

    private fun buildSampleMapExplorerMsgPackPayload(): ByteArray {
        val out = ByteArrayOutputStream()

        fun writeStr(s: String) {
            val bytes = s.toByteArray(Charsets.UTF_8)
            when {
                bytes.size <= 31 -> out.write(0xa0 or bytes.size)
                bytes.size <= 255 -> {
                    out.write(0xd9)
                    out.write(bytes.size)
                }
                else -> {
                    out.write(0xda)
                    out.write((bytes.size ushr 8) and 0xff)
                    out.write(bytes.size and 0xff)
                }
            }
            out.write(bytes)
        }

        fun writeInt(v: Int) {
            out.write(v and 0x7f)
        }

        fun writeUInt64(v: Long) {
            out.write(0xcf)
            out.write(ByteBuffer.allocate(8).putLong(v).array())
        }

        fun writeFloat64(v: Double) {
            out.write(0xcb)
            out.write(ByteBuffer.allocate(8).putDouble(v).array())
        }

        fun writeMapHeader(size: Int) {
            if (size <= 15) {
                out.write(0x80 or size)
            } else {
                out.write(0xde)
                out.write((size ushr 8) and 0xff)
                out.write(size and 0xff)
            }
        }

        // Top-level array of 2 reports (fixarray 2 = 0x92)
        out.write(0x92)

        // Report 1: 21 entries (same field order as backend MessagePack response)
        writeMapHeader(21)
        writeStr("id"); writeStr("SV-SSC-20260907-Dhly4eZs2nVGJ2gY")
        writeStr("lat"); writeFloat64(13.7269865)
        writeStr("lng"); writeFloat64(-89.1809165)
        writeStr("address"); writeStr("Area Urbana, Cuscatancingo, San Salvador, El Salvador")
        writeStr("title"); writeStr("Baches")
        writeStr("ocl"); writeStr("65GPRHC+QC")
        writeStr("description"); writeStr("Baches")
        writeStr("severityId"); writeInt(1)
        writeStr("statusId"); writeInt(2)
        writeStr("issueTypeId"); writeInt(1)
        writeStr("matterToSolveId"); writeInt(1)
        writeStr("cellIndex"); writeStr("8f6d75db4a21d12")
        writeStr("createdAtRaw"); writeUInt64(1788768837882L)
        writeStr("updatedAtRaw"); writeUInt64(1788768889064L)
        writeStr("userName"); writeStr("tests.reportamelo")
        writeStr("cityId"); writeStr("577119db-c0d2-474a-98fa-d58c8a7e307f")
        writeStr("attachments")
        out.write(0x91) // fixarray of 1 attachment
        writeMapHeader(11)
        writeStr("id"); writeStr("01a07aee-8a62-7661-abd0-bc064132f7dd")
        writeStr("type"); writeStr("image")
        writeStr("reportContainer"); writeStr("d8dcd05d-71be-45b3-bd65-d50af060c5da")
        writeStr("createdAtRaw"); writeUInt64(1788768837882L)
        writeStr("uploaderUserName"); writeStr("tests.reportamelo")
        writeStr("updatedAtRaw"); writeUInt64(1788768859734L)
        writeStr("validatedAtRaw"); writeUInt64(1788768859734L)
        writeStr("validatedBy"); writeStr("bot")
        writeStr("state"); writeStr("confirmed")
        writeStr("notes"); writeStr("Imagen muestra claramente los baches en la carretera.")
        writeStr("fileName"); writeStr("1788768837744-e346f1da-e2c8-4809-bf2a-9b95f86afc0a.webp")
        // Fields AFTER attachments in parent MapExplorerReport
        writeStr("shareUrl"); writeStr("/Dhly4eZs2nVGJ2gY/report/20260907/SSC/SV/baches")
        writeStr("reportContainer"); writeStr("d8dcd05d-71be-45b3-bd65-d50af060c5da")
        writeStr("profileId"); writeStr("1eEWAXwkcmYnUog")
        writeStr("observations"); writeStr("Las imágenes muestran claramente los baches reportados.")

        // Report 2: 22 entries (includes reportedAtRaw)
        writeMapHeader(22)
        writeStr("id"); writeStr("SV-SSC-20260906-40nPehqkecslz7GJ")
        writeStr("lat"); writeFloat64(13.725)
        writeStr("lng"); writeFloat64(-89.181)
        writeStr("address"); writeStr("3a Calle Oriente, Cuscatancingo, San Salvador, El Salvador")
        writeStr("title"); writeStr("Baches")
        writeStr("ocl"); writeStr("65GPRGC+Q2")
        writeStr("description"); writeStr("Baches")
        writeStr("severityId"); writeInt(1)
        writeStr("statusId"); writeInt(6)
        writeStr("issueTypeId"); writeInt(1)
        writeStr("matterToSolveId"); writeInt(1)
        writeStr("reportedAtRaw"); writeUInt64(1788768000000L)
        writeStr("cellIndex"); writeStr("8f6d75db4a19255")
        writeStr("createdAtRaw"); writeUInt64(1788767000000L)
        writeStr("updatedAtRaw"); writeUInt64(1788768000000L)
        writeStr("userName"); writeStr("tests.reportamelo")
        writeStr("cityId"); writeStr("577119db-c0d2-474a-98fa-d58c8a7e307f")
        writeStr("attachments"); out.write(0x90) // empty array
        writeStr("shareUrl"); writeStr("/40nPehqkecslz7GJ/report/20260906/SSC/SV/baches")
        writeStr("reportContainer"); writeStr("b66e2532-3503-45fd-9351-5a41f9713667")
        writeStr("profileId"); writeStr("1eEWAXwkcmYnUog")
        writeStr("observations"); writeStr("Observaciones")

        return out.toByteArray()
    }
}
