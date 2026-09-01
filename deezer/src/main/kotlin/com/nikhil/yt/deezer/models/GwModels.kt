package com.nikhil.yt.deezer.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class LoginResponse(
    val results: LoginResults = LoginResults(),
    val error: JsonElement? = null,
) {
    fun errorMessage(): String = error?.toString() ?: ""
}

@Serializable
data class LoginResults(
    val checkForm: String = "",
    val USER: LoginUser? = null,
)

@Serializable
data class LoginUser(
    val USER_ID: JsonElement = JsonPrimitive(""),
    val BLOG_NAME: String = "",
    val FIRSTNAME: String = "",
    val OPTIONS: LoginOptions? = null,
) {
    val userId: String get() = USER_ID.jsonPrimitive.content
    val name: String get() = BLOG_NAME.ifEmpty { FIRSTNAME }
    val options: LoginOptions? get() = OPTIONS
}

@Serializable
data class LoginOptions(
    val license_token: String = "",
    val web_hq: Boolean = false,
    val web_lossless: Boolean = false,
    val mobile_hq: Boolean = false,
    val mobile_lossless: Boolean = false,
)

@Serializable
data class SongDataResponse(
    val results: SongResults? = null,
    val error: JsonElement? = null,
) {
    fun errorMessage(): String = error?.toString() ?: ""
}

@Serializable
data class SongResults(
    val TRACK_TOKEN: String = "",
    val SNG_ID: JsonElement = JsonPrimitive(""),
    val SNG_TITLE: String = "",
    val DURATION: JsonElement = JsonPrimitive(""),
    val ART_NAME: String = "",
    val ALB_TITLE: String = "",
    val ALB_PICTURE: String = "",
    val GAIN: JsonElement = JsonPrimitive(""),
) {
    val trackToken: String get() = TRACK_TOKEN
}

@Serializable
data class MediaResponse(
    val data: List<MediaDataItem>? = null,
    val errors: List<MediaError>? = null,
) {
    fun errorMessage(): String =
        errors?.firstOrNull()?.message ?: data?.firstOrNull()?.errors?.firstOrNull()?.message ?: ""
}

@Serializable
data class MediaDataItem(
    val media: List<MediaItem>? = null,
    val errors: List<MediaError>? = null,
)

@Serializable
data class MediaItem(
    val format: String = "",
    val sources: List<MediaSource>? = null,
)

@Serializable
data class MediaSource(
    val url: String = "",
)

@Serializable
data class MediaError(
    val code: Int = 0,
    val message: String = "",
)
