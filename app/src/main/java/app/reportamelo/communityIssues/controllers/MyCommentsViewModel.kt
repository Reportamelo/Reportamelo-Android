package app.reportamelo.communityIssues.controllers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reportamelo.commons.SuccessfulResult
import app.reportamelo.models.Comment
import app.reportamelo.models.PaginatedResponse
import app.reportamelo.repository.CommentsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MyCommentsViewModel @JvmOverloads constructor(
    private val listByUserProvider: suspend (Int) -> PaginatedResponse<Comment> = { page ->
        CommentsRepository.listByUser(page)
    },
    private val deleteCommentProvider: suspend (String) -> SuccessfulResult = { id ->
        CommentsRepository.delete(id)
    }
) : ViewModel() {

    private val _paginatedResult = MutableStateFlow(
        PaginatedResponse<Comment>(
            documents = emptyList(),
            total = 0,
            page = 0,
            documentsPerPage = 0,
            totalPages = 0,
            hasNext = false,
            hasPrev = false
        )
    )
    val paginatedResult: StateFlow<PaginatedResponse<Comment>> = _paginatedResult.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun fetchComments(page: Int = 1) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = listByUserProvider(page)
                _paginatedResult.value = result
                val documents = result.documents ?: emptyList()
                if (page <= 1) {
                    _comments.value = documents
                } else {
                    _comments.update { existing -> existing + documents }
                }
            } catch (_: Exception) {
                // Keep current state when request fails, matching iOS CommentsSubView
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteComment(id: String) {
        viewModelScope.launch {
            try {
                val result = deleteCommentProvider(id)
                if (result == SuccessfulResult.DELETED) {
                    _comments.update { current ->
                        current.filterNot { it.id == id }
                    }
                }
            } catch (e: Exception) {
                println("Failed to delete comment: $e")
            }
        }
    }
}
