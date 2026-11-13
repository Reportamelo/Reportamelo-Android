package app.reportamelo.communityIssues.stubs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

class MockedDataTest {

    @Test
    fun mapExplorerMockedData_isSingletonAndContainsValidReport() {
        val instance1 = MapExplorerMockedData.shared
        val instance2 = MapExplorerMockedData.shared

        assertSame(instance1, instance2)
        assertNotNull(instance1.report)
        assertEquals("SV-SS-260601-aXWsaxls", instance1.report.id)
        assertEquals(2, instance1.report.attachments.size)
    }

    @Test
    fun selectedMockedCity_isSingletonAndContainsValidCity() {
        val instance1 = SelectedMockedCity.shared
        val instance2 = SelectedMockedCity.shared

        assertSame(instance1, instance2)
        assertNotNull(instance1.city)
        assertEquals("a67b90f9-1d76-4835-a994-03cd04f1d619", instance1.city.cityId)
    }
}
