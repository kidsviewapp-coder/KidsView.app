package why.xee.kidsview.data.repository

import why.xee.kidsview.data.model.RequestStatus
import why.xee.kidsview.data.model.RequestType
import why.xee.kidsview.data.model.VideoRequest
import why.xee.kidsview.utils.AuthHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRequestRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val collection = firestore.collection("video_requests")

    suspend fun createRequest(
        type: RequestType,
        videoName: String,
        videoId: String? = null,
        message: String = ""
    ): Result<VideoRequest> {
        return try {
            val userId = AuthHelper.requireUserId()
            val request = VideoRequest(
                requestId = UUID.randomUUID().toString(),
                type = type,
                videoName = videoName,
                videoId = videoId,
                message = message,
                userId = userId
            )
            collection.document(request.requestId)
                .set(request.toMap())
                .await()
            Result.success(request)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllRequests(): Result<List<VideoRequest>> {
        return try {
            val userId = AuthHelper.requireUserId()
            val snapshot = collection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val requests = snapshot.documents.mapNotNull { doc ->
                VideoRequest.fromDocument(doc)
            }
            Result.success(requests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRequestStatus(requestId: String, status: RequestStatus, rejectionMessage: String = ""): Result<Unit> {
        return try {
            val userId = AuthHelper.requireUserId()
            // Verify the request belongs to the current user
            val requestDoc = collection.document(requestId).get().await()
            val requestUserId = requestDoc.data?.get("userId") as? String
            if (requestUserId != userId) {
                return Result.failure(IllegalStateException("Cannot update request that belongs to another user"))
            }
            
            val updates = mutableMapOf<String, Any>("status" to status.name)
            if (rejectionMessage.isNotEmpty()) {
                updates["rejectionMessage"] = rejectionMessage
            }
            collection.document(requestId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPendingRequestCount(): Result<Int> {
        return try {
            val userId = AuthHelper.requireUserId()
            val snapshot = collection
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", RequestStatus.PENDING.name)
                .get()
                .await()
            Result.success(snapshot.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getRejectedRequests(): Result<List<VideoRequest>> {
        return try {
            val userId = AuthHelper.requireUserId()
            val snapshot = collection
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", RequestStatus.REJECTED.name)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val requests = snapshot.documents.mapNotNull { doc ->
                VideoRequest.fromDocument(doc)
            }
            Result.success(requests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRequest(requestId: String): Result<Unit> {
        return try {
            val userId = AuthHelper.requireUserId()
            // Verify the request belongs to the current user
            val requestDoc = collection.document(requestId).get().await()
            val requestUserId = requestDoc.data?.get("userId") as? String
            if (requestUserId != userId) {
                return Result.failure(IllegalStateException("Cannot delete request that belongs to another user"))
            }
            
            collection.document(requestId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

