package app.reportamelo.communityIssues.controllers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reportamelo.commons.CommentForType
import app.reportamelo.commons.CommonIntercommunicationErrors
import app.reportamelo.commons.KeychainToken
import app.reportamelo.commons.LegacyKeychain
import app.reportamelo.models.Comment
import app.reportamelo.models.GenericResponse
import app.reportamelo.models.PaginatedResponse
import app.reportamelo.repository.CommentsRepository
import app.reportamelo.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

class CommentsViewModel(
    private val commentFor: CommentForType,
    private val resourceId: String,
    private val getCommentsByReportProvider: suspend (String, Int) -> PaginatedResponse<Comment> = { id, page ->
        CommentsRepository.getCommentsByReport(id, page)
    },
    private val createCommentProvider: suspend (Comment) -> GenericResponse = { comment ->
        CommentsRepository.createCommentForReport(comment)
    },
    private val userNameProvider: () -> String = { UserRepository.shared.getName() },
    private val userAliasProvider: () -> String = { UserRepository.shared.getUsername() },
    private val userAvatarProvider: () -> String = { UserRepository.shared.getProfilePicture() },
    private val profileIdProvider: () -> String = { LegacyKeychain.getToken(KeychainToken.PROFILE_ID) ?: "" }
) : ViewModel() {

    private val _state = MutableStateFlow(CommentsState())
    val state: StateFlow<CommentsState> = _state.asStateFlow()

    fun updateCommentInput(text: String) {
        _state.update { it.copy(commentInput = text) }
    }

    fun dismissAlert() {
        _state.update { it.copy(presentAlert = false, message = "") }
    }

    fun fetchComments() {
        if (resourceId.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val result = getCommentsByReportProvider(resourceId, 1)
                val docs = result.documents ?: emptyList()
                _state.update {
                    it.copy(
                        paginatedResult = result,
                        comments = docs,
                        currentPage = 1,
                        canLoadMore = result.hasNext
                    )
                }
            } catch (e: Exception) {
                println("Error fetching comments: ${e.message}")
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun addComment() {
        val commentInput = _state.value.commentInput.trim()
        if (commentInput.isEmpty() || _state.value.isSubmitting) return

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, disableInput = true) }

            val comment = Comment(
                commentFor = commentFor,
                resourceId = resourceId,
                message = commentInput,
                name = userNameProvider(),
                userName = userAliasProvider(),
                profilePicture = userAvatarProvider(),
                profileId = profileIdProvider(),
                createdAt = Instant.now().toString()
            )

            try {
                val result = createCommentProvider(comment)
                if (result.code.isNotEmpty()) {
                    _state.update {
                        it.copy(comments = listOf(comment) + it.comments)
                    }
                }
            } catch (e: CommonIntercommunicationErrors.NetworkError) {
                _state.update {
                    it.copy(
                        message = e.message ?: "",
                        presentAlert = true
                    )
                }
            } catch (e: Exception) {
                println("Error adding comment: ${e.message}")
            } finally {
                _state.update {
                    it.copy(
                        disableInput = false,
                        commentInput = "",
                        isSubmitting = false
                    )
                }
            }
        }
    }

    fun loadMoreComments() {
        val currentState = _state.value
        if (currentState.isLoading || !currentState.canLoadMore) return

        val nextPage = currentState.currentPage + 1
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val result = getCommentsByReportProvider(resourceId, nextPage)
                val newComments = result.documents ?: emptyList()
                _state.update {
                    it.copy(
                        paginatedResult = result,
                        comments = it.comments + newComments,
                        currentPage = nextPage,
                        canLoadMore = result.hasNext
                    )
                }
            } catch (e: Exception) {
                println("Error loading more comments: ${e.message}")
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}

data class CommentsState(
    val commentInput: String = "",
    val comments: List<Comment> = emptyList(),
    val disableInput: Boolean = false,
    val isLoading: Boolean = false,
    val currentPage: Int = 1,
    val canLoadMore: Boolean = true,
    val isSubmitting: Boolean = false,
    val paginatedResult: PaginatedResponse<Comment> = PaginatedResponse(
        documents = emptyList(),
        total = 0,
        page = 0,
        documentsPerPage = 0,
        totalPages = 0,
        hasNext = false,
        hasPrev = false
    ),
    val limit: Int = 16,
    val animateInputIn: Boolean = false,
    val presentAlert: Boolean = false,
    val message: String = ""
)
