package app.reportamelo.communityIssues.controllers

import app.reportamelo.commons.CommentForType
import app.reportamelo.models.Comment
import app.reportamelo.models.GenericResponse
import app.reportamelo.models.PaginatedResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CommentsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fetchComments and loadMoreComments paginate and append comments`() = runTest(testDispatcher) {
        val page1Comment = Comment(
            id = "c1",
            commentFor = CommentForType.REPORT,
            resourceId = "rep-1",
            message = "First page comment"
        )
        val page2Comment = Comment(
            id = "c2",
            commentFor = CommentForType.REPORT,
            resourceId = "rep-1",
            message = "Second page comment"
        )

        val viewModel = CommentsViewModel(
            commentFor = CommentForType.REPORT,
            resourceId = "rep-1",
            getCommentsByReportProvider = { _, page ->
                if (page == 1) {
                    PaginatedResponse(
                        documents = listOf(page1Comment),
                        total = 2,
                        page = 1,
                        documentsPerPage = 1,
                        totalPages = 2,
                        hasNext = true,
                        hasPrev = false
                    )
                } else {
                    PaginatedResponse(
                        documents = listOf(page2Comment),
                        total = 2,
                        page = 2,
                        documentsPerPage = 1,
                        totalPages = 2,
                        hasNext = false,
                        hasPrev = true
                    )
                }
            }
        )

        viewModel.fetchComments()
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.comments.size)
        assertEquals(1, viewModel.state.value.currentPage)
        assertTrue(viewModel.state.value.canLoadMore)

        viewModel.loadMoreComments()
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.comments.size)
        assertEquals("First page comment", viewModel.state.value.comments[0].message)
        assertEquals("Second page comment", viewModel.state.value.comments[1].message)
        assertEquals(2, viewModel.state.value.currentPage)
        assertFalse(viewModel.state.value.canLoadMore)
    }

    @Test
    fun `addComment prepends new comment with user metadata and clears input`() = runTest(testDispatcher) {
        var createdComment: Comment? = null
        val viewModel = CommentsViewModel(
            commentFor = CommentForType.REPORT,
            resourceId = "rep-1",
            createCommentProvider = { comment ->
                createdComment = comment
                GenericResponse(id = "NEW_COMMENT", message = "Created", code = "COMMENT_CREATED")
            },
            userNameProvider = { "Test Citizen" },
            userAliasProvider = { "test.citizen" },
            userAvatarProvider = { "/avatars/custom.png" },
            profileIdProvider = { "prof-777" }
        )

        viewModel.updateCommentInput("There is a huge pothole blocking the lane")
        viewModel.addComment()
        advanceUntilIdle()

        assertEquals("", viewModel.state.value.commentInput)
        assertEquals(1, viewModel.state.value.comments.size)
        assertEquals("There is a huge pothole blocking the lane", createdComment?.message)
        assertEquals("Test Citizen", createdComment?.name)
        assertEquals("test.citizen", createdComment?.userName)
        assertEquals("prof-777", createdComment?.profileId)
    }
}
