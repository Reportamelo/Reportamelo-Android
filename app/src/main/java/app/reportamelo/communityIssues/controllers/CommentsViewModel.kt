package app.reportamelo.communityIssues.controllers

import app.reportamelo.commons.utils.*
import app.reportamelo.communityIssues.datamodel.*

import app.reportamelo.commons.*
import app.reportamelo.models.*
import app.reportamelo.repository.*
import app.reportamelo.network.services.*
import app.reportamelo.network.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reportamelo.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CommentsViewModel(
    private val commentFor: CommentForType,
    private val resourceId: String
) : ViewModel() {

    private val _state = MutableStateFlow(CommentsState())
    val state: StateFlow<CommentsState> = _state.asStateFlow()

    fun updateCommentInput(text: String) {
        _state.update { it.copy(commentInput = text) }
    }

    fun fetchComments() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val limit = _state.value.limit
                val page = _state.value.currentPage
                val result = CommentsRepository.getCommentsByReport(resourceId, page)
                
                _state.update { 
                    it.copy(
                        paginatedResult = result,
                        comments = result.documents ?: emptyList()
                    )
                }
            } catch (e: Exception) {
                println(e)
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun addComment() {
        val commentInput = _state.value.commentInput
        if (commentInput.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, disableInput = true) }
            
            val comment = Comment(
                commentFor = commentFor,
                resourceId = resourceId,
                message = commentInput,
                name = "",
                userName = "",
                profilePicture = "",
                profileId = "",
                createdAt = ""
            )
            
            try {
                val result = CommentsRepository.createCommentForReport(comment)
                if (result.code != null && result.code.isNotEmpty()) {
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
                println(e)
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

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val result = CommentsRepository.getCommentsByReport(resourceId, currentState.currentPage)
                
                result.documents?.let { newComments ->
                    // dump(newComments)
                }
            } catch (e: Exception) {
                println(e)
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
