package app.reportamelo.communityIssues.controllers

import app.reportamelo.communityIssues.datamodel.LicenseCatalog
import app.reportamelo.communityIssues.datamodel.LicenseSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LicensesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private class FakeSource(
        private val files: Map<String, String?>
    ) : LicenseSource {
        override fun list(): List<String> = files.keys.toList()
        override fun read(fileName: String): String =
            files[fileName] ?: throw java.io.IOException("unreadable: $fileName")
    }

    private val apache = """
                                 Apache License
                           Version 2.0, January 2004

   1. Definitions.

      "License" shall mean the terms and conditions for use,
      reproduction, and distribution.
    """.trimIndent()

    private val credits = """
        Photo by <a href="https://unsplash.com/@a">A</a> on <a href="https://unsplash.com">Unsplash</a>


        Photo by <a href="https://unsplash.com/@b">B</a> on <a href="https://unsplash.com">Unsplash</a>
    """.trimIndent()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `reflow joins hard-wrapped lines and keeps blank-line paragraphs`() {
        assertEquals(
            listOf(
                "Apache License Version 2.0, January 2004",
                "1. Definitions.",
                "\"License\" shall mean the terms and conditions for use, reproduction, and distribution."
            ),
            LicenseCatalog.reflow(apache)
        )
        assertEquals(listOf("a b", "c"), LicenseCatalog.reflow("a\r\nb\r\n\r\n  \r\nc\r\n"))
    }

    @Test
    fun `catalog names entries from file names, sorts licenses then credits and ignores other files`() {
        val source = FakeSource(
            mapOf(
                "Unsplash.CREDITS.txt" to credits,
                "ktor.LICENSE.txt" to apache,
                "README.md" to "not a license",
                "activity-compose.LICENSE.txt" to apache,
                "Coil.LICENSE.txt" to apache
            )
        )

        val licenses = LicenseCatalog.load(source, unavailableText = "n/a")

        assertEquals(listOf("activity-compose", "Coil", "ktor", "Unsplash"), licenses.map { it.library })
        assertEquals("activity-compose.LICENSE.txt", licenses.first().id)
        val unsplash = licenses.last()
        assertTrue(unsplash.isCredit)
        assertTrue(unsplash.isHtml)
        assertEquals(2, unsplash.paragraphs.size)
        assertFalse(licenses.first().isHtml)
    }

    @Test
    fun `an unreadable or empty file is still listed with the unavailable text`() {
        val source = FakeSource(mapOf("broken.LICENSE.txt" to null, "empty.LICENSE.txt" to "  \n\n "))

        val licenses = LicenseCatalog.load(source, unavailableText = "n/a")

        assertEquals(listOf("broken", "empty"), licenses.map { it.library })
        assertTrue(licenses.all { it.paragraphs == listOf("n/a") })
    }

    @Test
    fun `starts with every license expanded like iOS and keeps the toggle-all state in sync`() {
        val source = FakeSource(mapOf("a.LICENSE.txt" to apache, "b.LICENSE.txt" to apache))
        val viewModel = LicensesViewModel(source, ioDispatcher = testDispatcher)

        val loaded = viewModel.state.value
        assertFalse(loaded.isLoading)
        assertEquals(setOf("a.LICENSE.txt", "b.LICENSE.txt"), loaded.expandedIds)
        assertTrue(loaded.allExpanded)

        // Collapsing one item turns the toolbar button into "expand all"
        viewModel.toggle("a.LICENSE.txt")
        assertFalse(viewModel.state.value.allExpanded)

        // ...so the next toggle-all expands everything instead of collapsing
        viewModel.toggleAll()
        assertTrue(viewModel.state.value.allExpanded)

        viewModel.toggleAll()
        assertTrue(viewModel.state.value.expandedIds.isEmpty())
        assertFalse(viewModel.state.value.allExpanded)

        // Re-expanding items one by one brings it back to "all expanded"
        viewModel.toggle("a.LICENSE.txt")
        viewModel.toggle("b.LICENSE.txt")
        assertTrue(viewModel.state.value.allExpanded)
    }

    @Test
    fun `no license files means an empty, never all-expanded state`() {
        val viewModel = LicensesViewModel(FakeSource(emptyMap()), ioDispatcher = testDispatcher)

        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.licenses.isEmpty())
        assertFalse(viewModel.state.value.allExpanded)
    }
}
