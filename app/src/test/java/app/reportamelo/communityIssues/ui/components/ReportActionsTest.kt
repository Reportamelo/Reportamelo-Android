package app.reportamelo.communityIssues.ui.components

import app.reportamelo.commons.IssueStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportActionsTest {

    @Test
    fun `delete is offered only on REJECTED and RECTIFICATION`() {
        IssueStatus.entries.forEach { status ->
            val expected =
                status == IssueStatus.REJECTED || status == IssueStatus.RECTIFICATION
            assertEquals("status=$status", expected, canDeleteReport(status))
        }
    }

    @Test
    fun `the delete row is omitted, not disabled, when the status forbids it`() {
        val notDeletable = IssueStatus.entries.filterNot { canDeleteReport(it) }

        notDeletable.forEach { status ->
            assertFalse("status=$status", ReportAction.DELETE in reportActionsFor(status))
        }
    }

    @Test
    fun `delete goes last, after the four always-available actions`() {
        val always = listOf(
            ReportAction.OPEN_IN_MAPS,
            ReportAction.COPY_ID,
            ReportAction.SHARE_REPORT,
            ReportAction.TIMELINE
        )

        assertEquals(always, reportActionsFor(IssueStatus.FIXED))
        assertEquals(always + ReportAction.DELETE, reportActionsFor(IssueStatus.REJECTED))
        assertEquals(always + ReportAction.DELETE, reportActionsFor(IssueStatus.RECTIFICATION))
    }

    @Test
    fun `every status offers the four non-destructive actions`() {
        IssueStatus.entries.forEach { status ->
            val actions = reportActionsFor(status)
            assertTrue("status=$status", actions.containsAll(listOf(
                ReportAction.OPEN_IN_MAPS,
                ReportAction.COPY_ID,
                ReportAction.SHARE_REPORT,
                ReportAction.TIMELINE
            )))
        }
    }
}
