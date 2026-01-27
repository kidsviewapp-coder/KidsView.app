package why.xee.kidsview.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Firestore model for video categories
 * Collection: users/{userId}/categories/{categoryId}
 * Each user has their own private category collection
 */
data class FirestoreCategory(
    val categoryId: String,
    val name: String,
    val description: String = "",
    val icon: String = "📁", // Emoji icon
    val color: String = "#6C5CE7", // Hex color
    val timestamp: Long = System.currentTimeMillis(),
    val videoCount: Int = 0
) {
    /**
     * Convert to Firestore Map
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "categoryId" to categoryId,
            "name" to name,
            "description" to description,
            "icon" to icon,
            "color" to color,
            "videoCount" to videoCount,
            "timestamp" to Timestamp(timestamp / 1000, ((timestamp % 1000) * 1000000).toInt())
        )
    }

    companion object {
        /**
         * Generate categoryId from name
         */
        fun generateCategoryId(name: String): String {
            return name.lowercase()
                .replace(" ", "_")
                .replace(Regex("[^a-z0-9_]"), "")
                .take(50)
        }

        /**
         * Convert from Firestore DocumentSnapshot
         */
        fun fromDocument(document: DocumentSnapshot): FirestoreCategory? {
            return try {
                val data = document.data ?: return null
                val timestamp = when (val ts = data["timestamp"]) {
                    is Timestamp -> ts.toDate()?.time ?: System.currentTimeMillis()
                    is Long -> ts
                    is Number -> ts.toLong()
                    else -> System.currentTimeMillis()
                }

                FirestoreCategory(
                    categoryId = data["categoryId"] as? String ?: document.id,
                    name = data["name"] as? String ?: "",
                    description = data["description"] as? String ?: "",
                    icon = data["icon"] as? String ?: "📁",
                    color = data["color"] as? String ?: "#6C5CE7",
                    videoCount = (data["videoCount"] as? Number)?.toInt() ?: 0,
                    timestamp = timestamp
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

