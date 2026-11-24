package com.huanchengfly.tieba.post.models

import com.google.gson.annotations.SerializedName
import com.huanchengfly.tieba.post.utils.GsonUtil

/**
 * Structured rich draft holding both text, attached images, origin image setting and update timestamp.
 */
data class DraftModel(
    @SerializedName("text")
    val text: String = "",
    @SerializedName("images")
    val imageUris: List<String> = emptyList(),
    @SerializedName("is_origin")
    val isOriginImage: Boolean = false,
    @SerializedName("timestamp")
    val updateTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * True if there is neither text nor any attached images.
     */
    fun isBlank(): Boolean = text.isBlank() && imageUris.isEmpty()

    /**
     * Serialize to JSON string for persistent database storage.
     */
    fun serialize(): String {
        return GsonUtil.getGson().toJson(this)
    }

    companion object {
        /**
         * Deserializes either a rich JSON draft or a legacy plain-text draft.
         */
        fun deserialize(raw: String?): DraftModel {
            if (raw.isNullOrBlank()) return DraftModel()
            val trimmed = raw.trim()
            if (trimmed.startsWith("{") && trimmed.contains("\"text\"")) {
                return runCatching {
                    GsonUtil.getGson().fromJson(trimmed, DraftModel::class.java)
                }.getOrElse {
                    DraftModel(text = raw)
                }
            }
            return DraftModel(text = raw)
        }
    }
}
