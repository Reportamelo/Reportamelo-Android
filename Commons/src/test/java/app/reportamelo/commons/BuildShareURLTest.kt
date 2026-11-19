package app.reportamelo.commons

import app.reportamelo.commons.utils.buildShareURLString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BuildShareURLTest {

    private val host = "https://${BuildConfig.SHARE_URL}"

    @Test
    fun `joins the share path to the share host without encoding slashes`() {
        assertEquals(
            "$host/A9wWx8pwePnr25UE/report/20260907/SSC/SV/lamparas-quemadas",
            buildShareURLString("A9wWx8pwePnr25UE/report/20260907/SSC/SV/lamparas-quemadas")
        )
    }

    @Test
    fun `tolerates a leading slash and surrounding whitespace`() {
        assertEquals(
            "$host/A9wWx8pwePnr25UE/report/20260907/SSC/SV/lamparas-quemadas",
            buildShareURLString(" /A9wWx8pwePnr25UE/report/20260907/SSC/SV/lamparas-quemadas ")
        )
    }

    @Test
    fun `keeps absolute urls untouched`() {
        assertEquals(
            "https://share.reportamelo.app/abc/report/x",
            buildShareURLString("https://share.reportamelo.app/abc/report/x")
        )
    }

    @Test
    fun `returns null when there is no path`() {
        assertNull(buildShareURLString(null))
        assertNull(buildShareURLString("  "))
    }
}
