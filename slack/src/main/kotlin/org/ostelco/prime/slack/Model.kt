package org.ostelco.prime.slack

import com.fasterxml.jackson.annotation.JsonProperty

data class Message(
        val channel: String,
        @JsonProperty("username") val userName: String? = null,
        val text: String = "",
        @JsonProperty("icon_emoji") val iconEmoji: String? = null,
        val attachments: List<Attachment> = emptyList()) {

    fun format(): Message = this.copy(
            channel = "#$channel",
            text = "<!channel> $text",
            iconEmoji = iconEmoji?.let { ":$it:" })
}

data class Attachment(
        val fallback: String,
        val color: String? = null,
        val pretext: String? = null,
        @JsonProperty("author_name") val authorName: String,
        @JsonProperty("author_link") val authorLink: String? = null,
        @JsonProperty("author_icon") val authorIcon: String? = null,
        val title: String,
        @JsonProperty("title_link") val titleLink: String? = null,
        val text: String,
        val fields: List<Field> = emptyList(),
        @JsonProperty("image_url") val imageUrl: String? = null,
        @JsonProperty("thumb_url") val thumbUrl: String? = null,
        val footer: String? = null,
        @JsonProperty("footer_icon") val footerIcon: String? = null,
        @JsonProperty("ts") val timestampEpochSeconds: Long)

data class Field(
        val title: String,
        val value: String,
        val short: Boolean)