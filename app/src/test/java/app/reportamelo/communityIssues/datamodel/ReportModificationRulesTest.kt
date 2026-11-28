package app.reportamelo.communityIssues.datamodel

import app.reportamelo.commons.IssueStatus
import app.reportamelo.commons.ViewOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ReportModificationRulesTest {

    @Test
    fun `only REPORTED and RECTIFICATION are editable in LIST_AND_MODIFY`() {
        IssueStatus.entries.forEach { status ->
            val expected = status == IssueStatus.REPORTED || status == IssueStatus.RECTIFICATION
            assertEquals("status=$status", expected, canModifyReport(ViewOptions.LIST_AND_MODIFY, status))
        }
    }

    @Test
    fun `nothing is editable in LIST mode`() {
        IssueStatus.entries.forEach { status ->
            assertFalse("status=$status", canModifyReport(ViewOptions.LIST, status))
        }
    }
}
