package app.reportamelo.commons


enum class ViewOptions(val value: String) {
    LIST("list"),
    LIST_AND_MODIFY("listAndModify")
}

enum class AuthMethod(val value: String) {
    APPLE("Apple"),
    GOOGLE("Google")
}

sealed class LoginType {
    object Guest : LoginType()
    data class User(val authMethod: AuthMethod) : LoginType()
}

enum class TextBasedAvatarOptions(val value: String) {
    MONOGRAM("monogram"),
    INITIALS("initials")
}

enum class MonogramMode(val value: String) {
    PREVIEW("preview"),
    SEND("send")
}

sealed class CommonIntercommunicationErrors : Exception() {
    object Delayed : CommonIntercommunicationErrors()
    object TimedOut : CommonIntercommunicationErrors()
    object Removed : CommonIntercommunicationErrors()
    object NotFound : CommonIntercommunicationErrors()
    data class InvalidPetition(override val message: String) : CommonIntercommunicationErrors()
    data class ServerError(override val message: String) : CommonIntercommunicationErrors()
    object NotAuthorized : CommonIntercommunicationErrors()
    data class Forbidden(override val message: String) : CommonIntercommunicationErrors()
    data class NetworkError(override val message: String) : CommonIntercommunicationErrors()
    data class GenericError(override val message: String) : CommonIntercommunicationErrors()
    object NotImplemented : CommonIntercommunicationErrors()
    object UnProcessable : CommonIntercommunicationErrors()
}

enum class SuccessfulResult {
    DONE,
    UPDATED,
    DELETED,
    CREATED
}

enum class UserNameState {
    UPDATED,
    UNTOUCHED,
    ERROR
}

enum class InteractionType(val value: String) {
    VIEW_AD("viewAd"),
    PAID_A_SUBSCRIPTION("paidASubscription"),
    NO_THANKS("noThanks")
}

enum class PlanType(val value: String) {
    FREEMIUM("freemium"),
    PAID("paid"),
    REVENUE_AD("revenueAd"),
    FREEMIUM_FOR_GUESTS("freemiumForGuests")
}
