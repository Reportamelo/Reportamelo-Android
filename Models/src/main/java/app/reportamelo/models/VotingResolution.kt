package app.reportamelo.models

import kotlinx.serialization.Serializable

@Serializable
data class VotingResolution(
    val hasVoted: Boolean,
    val voteCount: Int
)
