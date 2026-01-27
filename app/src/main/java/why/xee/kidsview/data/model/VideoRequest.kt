package why.xee.kidsview.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Model for kid video requests (add or delete)
 */
data class VideoRequest(
    val requestId: String,
    val type: RequestType, // ADD or DELETE
    val videoName: String, // Video name provided by kid
    val videoId: String? = null, // Video ID if it's a delete request
    val message: String = "", // Optional message from kid
    val timestamp: Long = System.currentTimeMillis(),
    val status: RequestStatus = RequestStatus.PENDING, // PENDING, APPROVED, REJECTED
    val rejectionMessage: String = "", // Message from parent when rejecting
    val userId: String = "" // User ID to isolate requests per user
) {
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "requestId" to requestId,
            "type" to type.name,
            "videoName" to videoName,
            "message" to message,
            "status" to status.name,
            "timestamp" to Timestamp(timestamp / 1000, ((timestamp % 1000) * 1000000).toInt()),
            "rejectionMessage" to rejectionMessage,
            "userId" to userId
        )
        videoId?.let { map["videoId"] = it }
        return map
    }

    companion object {
        fun fromDocument(document: DocumentSnapshot): VideoRequest? {
            return try {
                val data = document.data ?: return null
                val timestamp = when (val ts = data["timestamp"]) {
                    is Timestamp -> ts.toDate()?.time ?: System.currentTimeMillis()
                    is Long -> ts
                    is Number -> ts.toLong()
                    else -> System.currentTimeMillis()
                }
                
                VideoRequest(
                    requestId = document.id,
                    type = RequestType.valueOf(data["type"] as? String ?: "ADD"),
                    videoName = data["videoName"] as? String ?: "",
                    videoId = data["videoId"] as? String,
                    message = data["message"] as? String ?: "",
                    timestamp = timestamp,
                    status = RequestStatus.valueOf(data["status"] as? String ?: "PENDING"),
                    rejectionMessage = data["rejectionMessage"] as? String ?: "",
                    userId = data["userId"] as? String ?: ""
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

enum class RequestType {
    ADD,    // Request to add a video
    DELETE  // Request to delete a video
}

enum class RequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}

