package app.reportamelo.commons

data class Validator(
    val name: String,
    val message: String,
    val fn: (String) -> Boolean
)

const val TITLE_MIN_CHARACTERS: Int = 5
const val TITLE_MAX_CHARACTERS: Int = 40
val titleValidator: List<Validator> = listOf(
    Validator(
        name = "Isn't empty",
        message = "The title can't be empty",
        fn = { it.isNotEmpty() }
    ),
    Validator(
        name = "Isn't too long",
        message = "The title can't be longer than $TITLE_MAX_CHARACTERS characters",
        fn = { it.length <= TITLE_MAX_CHARACTERS }
    ),
    Validator(
        name = "Isn't too short",
        message = "The title can't be shorter than $TITLE_MIN_CHARACTERS characters",
        fn = { it.length >= TITLE_MIN_CHARACTERS }
    )
)

const val DESCRIPTION_MIN_CHARACTERS: Int = 5
const val DESCRIPTION_MAX_CHARACTERS: Int = 150
val descriptionValidator: List<Validator> = listOf(
    Validator(
        name = "Isn't empty",
        message = "The description can't be empty",
        fn = { it.isNotEmpty() }
    ),
    Validator(
        name = "Isn't too long",
        message = "The description can't be longer than $DESCRIPTION_MAX_CHARACTERS characters",
        fn = { it.length <= DESCRIPTION_MAX_CHARACTERS }
    ),
    Validator(
        name = "Isn't too short",
        message = "The description can't be shorter than $DESCRIPTION_MIN_CHARACTERS characters",
        fn = { it.length >= DESCRIPTION_MIN_CHARACTERS }
    )
)

const val ADDRESS_MIN_CHARACTERS: Int = 5
const val ADDRESS_MAX_CHARACTERS: Int = 200
val addressValidator: List<Validator> = listOf(
    Validator(
        name = "Isn't too long",
        message = "The address can't be longer than $ADDRESS_MAX_CHARACTERS characters",
        fn = { it.length <= ADDRESS_MAX_CHARACTERS }
    ),
    Validator(
        name = "Isn't too short",
        message = "The address can't be shorter than $ADDRESS_MIN_CHARACTERS characters",
        fn = { it.length >= ADDRESS_MIN_CHARACTERS }
    )
)

const val ADDRESS_REGEX = "[\\p{L}0-9\\s,.\\-#]"

val emailValidator: List<Validator> = listOf(
    Validator(
        name = "Validate email",
        message = "Your email is invalid",
        fn = { value ->
            val emailRegex = "^[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}$".toRegex(RegexOption.IGNORE_CASE)
            emailRegex.matches(value)
        }
    )
)

const val USER_NAME_MIN_CHARACTERS: Int = 3
const val USER_NAME_MAX_CHARACTERS: Int = 20
val userNameValidator: List<Validator> = listOf(
    Validator(
        name = "Isn't empty",
        message = "User name can't be empty",
        fn = { it.isNotEmpty() }
    ),
    Validator(
        name = "Isn't too long",
        message = "User name can't be longer than $USER_NAME_MAX_CHARACTERS characters",
        fn = { it.length <= USER_NAME_MAX_CHARACTERS }
    ),
    Validator(
        name = "Isn't too short",
        message = "User name can't be shorter than $USER_NAME_MIN_CHARACTERS characters",
        fn = { it.length >= USER_NAME_MIN_CHARACTERS }
    )
)

const val USER_NAME_REGEX = "[a-zA-Z0-9._-]"

const val BLOCK_REASON_MIN_CHARACTERS: Int = 12
const val BLOCK_REASON_MAX_CHARACTERS: Int = 150
val blockReasonValidator: List<Validator> = listOf(
    Validator(
        name = "Isn't empty",
        message = "The reason can't be empty",
        fn = { it.isNotEmpty() }
    ),
    Validator(
        name = "Isn't too long",
        message = "The reason can't be longer than $BLOCK_REASON_MAX_CHARACTERS characters",
        fn = { it.length <= BLOCK_REASON_MAX_CHARACTERS }
    ),
    Validator(
        name = "Isn't too short",
        message = "The reason can't be shorter than $BLOCK_REASON_MIN_CHARACTERS characters",
        fn = { it.length >= BLOCK_REASON_MIN_CHARACTERS }
    )
)

fun isAValidText(text: String): Boolean {
    val pattern = "^[a-zA-Z0-9\\. -#]+$".toRegex()
    return pattern.matches(text)
}

object LocationValidator {
    // Empty as in the Swift version
}
