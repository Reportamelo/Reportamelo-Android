package app.reportamelo.commons.validators

import android.util.Patterns

const val TITLE_MIN_CHARACTERS = 5
const val TITLE_MAX_CHARACTERS = 40

val titleValidator = listOf(
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

const val DESCRIPTION_MIN_CHARACTERS = 5
const val DESCRIPTION_MAX_CHARACTERS = 150

val descriptionValidator = listOf(
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

const val ADDRESS_MIN_CHARACTERS = 5
const val ADDRESS_MAX_CHARACTERS = 200

val addressValidator = listOf(
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

val emailValidator = listOf(
    Validator(
        name = "Validate email",
        message = "Your email is invalid",
        fn = { Patterns.EMAIL_ADDRESS.matcher(it).matches() }
    )
)

const val USER_NAME_MIN_CHARACTERS = 3
const val USER_NAME_MAX_CHARACTERS = 20

val userNameValidator = listOf(
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

const val BLOCK_REASON_MIN_CHARACTERS = 12
const val BLOCK_REASON_MAX_CHARACTERS = 150

val blockReasonValidator = listOf(
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
