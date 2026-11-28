package app.reportamelo.models

import app.reportamelo.commons.AttachmentType
import app.reportamelo.commons.ReportAttachmentState
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResolutionSerializationTest {

    /** Misma configuración que `ServiceClient.json`. */
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    /**
     * Respuesta real de `fetchResolutionByReport`: el adjunto no trae `notes` y los metadatos
     * traen `metrics`, que el modelo no conoce.
     */
    private val resolutionPayload = """
        {
          "id": "dfdebdfc-19ec-468d-b6f2-5c232cf8127a",
          "status": "started",
          "history": {
            "confirmed": {
              "attachments": [],
              "by": "Reportamelo Bot",
              "comments": "Report confirmed",
              "date": "2026-11-21",
              "status": "confirmed"
            },
            "reported": {
              "attachments": [
                {
                  "attachmentId": "9dc54beb-1413-4fa3-a6ff-fa3a567cf77f",
                  "type": "image",
                  "reportContainer": "9306f327-bd38-4907-ad69-5d70a139b1b6",
                  "createdAt": 1795238584596,
                  "uploaderUserName": "tests.reportamelo",
                  "state": "pending",
                  "key": "attachments/review/9306f327-bd38-4907-ad69-5d70a139b1b6/1795232160985-879a8c84.webp",
                  "fileName": "1795232160985-879a8c84.webp"
                }
              ],
              "by": "ba52f168-f9cf-465c-8a1e-de7d84a869ba",
              "comments": "se confirma la gravedad de este reporte",
              "date": "2026-11-21",
              "status": "reported"
            }
          },
          "metadata": {
            "cityId": "577119db-c0d2-474a-98fa-d58c8a7e307f",
            "groupingId": "f30b2834-12ee-4b0c-85eb-660ee7b08ed4",
            "resourceType": "report",
            "resourceId": "SV-SSC-20261120-LleqAJRZ3mpZVdhs",
            "assigned": {
              "institutionName": "MOP, Ministerio de Obras Públicas",
              "institutionCode": "MOP",
              "institutionId": "019f7d71-ad1b-75c4-b2c7-2f53d381cb75"
            },
            "metrics": { "reports": 1, "takenDays": 0, "totalRepairs": 0 }
          }
        }
    """.trimIndent()

    @Test
    fun `an attachment without notes decodes with an empty note`() {
        val resolution = json.decodeFromString<Resolution>(resolutionPayload)

        val attachment = resolution.history.reported?.attachments?.single()
        assertEquals("", attachment?.notes)
        assertEquals("9dc54beb-1413-4fa3-a6ff-fa3a567cf77f", attachment?.id)
        assertEquals(AttachmentType.IMAGE, attachment?.type)
        assertEquals(ReportAttachmentState.PENDING, attachment?.state)
        assertEquals(1795238584596L, attachment?.createdAtRaw)
    }

    @Test
    fun `an explicit null note is coerced to the default`() {
        val payload = resolutionPayload.replace(
            "\"state\": \"pending\",",
            "\"state\": \"pending\",\n                  \"notes\": null,"
        )

        val resolution = json.decodeFromString<Resolution>(payload)

        assertEquals("", resolution.history.reported?.attachments?.single()?.notes)
    }

    @Test
    fun `the rest of the resolution survives unknown fields`() {
        val resolution = json.decodeFromString<Resolution>(resolutionPayload)

        assertEquals("dfdebdfc-19ec-468d-b6f2-5c232cf8127a", resolution.id)
        assertEquals("started", resolution.status)
        assertEquals("2026-11-21", resolution.history.reported?.date)
        assertEquals("Reportamelo Bot", resolution.history.confirmed?.by)
        assertEquals("MOP", resolution.metadata.assigned.institutionCode)
        assertNull(resolution.history.fixed)
        assertNull(resolution.history.inProgress)
    }
}
