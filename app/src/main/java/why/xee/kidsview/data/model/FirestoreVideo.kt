package why.xee.kidsview.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Firestore model for parent-selected videos
 * Collection: users/{userId}/videos/{videoId}
 * Each user has their own private video collection
 */
data class FirestoreVideo(
    val videoId: String,
    val title: String,
    val thumbnail: String,
    val duration: String = "",
    val channel: String,
    val timestamp: Long = System.currentTimeMillis(),
    val quality: String = "auto", // auto, hd720, medium, small
    val categoryId: String? = null // Reference to category
) {
    /**
     * Convert to Firestore Map
     */
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "videoId" to videoId,
            "title" to title,
            "thumbnail" to thumbnail,
            "duration" to duration,
            "channel" to channel,
            "quality" to quality,
            "timestamp" to Timestamp(timestamp / 1000, ((timestamp % 1000) * 1000000).toInt())
        )
        categoryId?.let { map["categoryId"] = it }
        return map
    }

    companion object {
        /**
         * Convert from Firestore DocumentSnapshot
         */
        fun fromDocument(document: DocumentSnapshot): FirestoreVideo? {
            return try {
                val data = document.data ?: return null
                val timestamp = when (val ts = data["timestamp"]) {
                    is Timestamp -> ts.toDate()?.time ?: System.currentTimeMillis()
                    is Long -> ts
                    is Number -> ts.toLong()
                    else -> System.currentTimeMillis()
                }
                
                FirestoreVideo(
                    videoId = data["videoId"] as? String ?: document.id,
                    title = data["title"] as? String ?: "",
                    thumbnail = data["thumbnail"] as? String ?: "",
                    duration = data["duration"] as? String ?: "",
                    channel = data["channel"] as? String ?: "",
                    quality = data["quality"] as? String ?: "auto",
                    categoryId = data["categoryId"] as? String,
                    timestamp = timestamp
                )
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Convert from VideoItem (for compatibility)
         */
        fun fromVideoItem(
            videoItem: VideoItem, 
            duration: String = "",
            quality: String = "auto",
            categoryId: String? = null
        ): FirestoreVideo {
            return FirestoreVideo(
                videoId = videoItem.id,
                title = videoItem.title,
                thumbnail = videoItem.thumbnailUrl,
                duration = duration,
                channel = videoItem.channelTitle,
                quality = quality,
                categoryId = categoryId,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}

