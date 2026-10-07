package app.reportamelo.models

import app.reportamelo.commons.VotingType
import kotlinx.serialization.Serializable

@Serializable
data class Voting(
    val type: VotingType,
    val resourceId: String
)
