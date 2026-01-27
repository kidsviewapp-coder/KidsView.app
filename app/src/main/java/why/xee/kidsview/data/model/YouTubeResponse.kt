package why.xee.kidsview.data.model

import com.google.gson.annotations.SerializedName

/**
 * Response model for YouTube Data API v3
 */
data class YouTubeResponse(
    @SerializedName("items")
    val items: List<YouTubeItem>?,
    @SerializedName("nextPageToken")
    val nextPageToken: String?,
    @SerializedName("error")
    val error: YouTubeError?
)

/**
 * Error response from YouTube API
 */
data class YouTubeError(
    @SerializedName("code")
    val code: Int?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("errors")
    val errors: List<YouTubeErrorDetail>?
)

data class YouTubeErrorDetail(
    @SerializedName("message")
    val message: String?,
    @SerializedName("domain")
    val domain: String?,
    @SerializedName("reason")
    val reason: String?
)

data class YouTubeItem(
    @SerializedName("id")
    val id: YouTubeItemId?,
    @SerializedName("snippet")
    val snippet: YouTubeSnippet
)

data class YouTubeItemId(
    @SerializedName("videoId")
    val videoId: String?,
    @SerializedName("playlistId")
    val playlistId: String?
)

data class YouTubeSnippet(
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("channelTitle")
    val channelTitle: String,
    @SerializedName("thumbnails")
    val thumbnails: YouTubeThumbnails,
    @SerializedName("publishedAt")
    val publishedAt: String,
    @SerializedName("resourceId")
    val resourceId: YouTubeResourceId? // For playlistItems
)

data class YouTubeResourceId(
    @SerializedName("videoId")
    val videoId: String?
)

data class YouTubeThumbnails(
    @SerializedName("default")
    val default: YouTubeThumbnail?,
    @SerializedName("medium")
    val medium: YouTubeThumbnail?,
    @SerializedName("high")
    val high: YouTubeThumbnail?
)

data class YouTubeThumbnail(
    @SerializedName("url")
    val url: String,
    @SerializedName("width")
    val width: Int,
    @SerializedName("height")
    val height: Int
)

/**
 * Response model for YouTube Video Details API
 */
data class YouTubeVideoDetailsResponse(
    @SerializedName("items")
    val items: List<YouTubeVideoDetail>?,
    @SerializedName("error")
    val error: YouTubeError?
)

/**
 * Video detail item with content details
 */
data class YouTubeVideoDetail(
    @SerializedName("id")
    val id: String,
    @SerializedName("snippet")
    val snippet: YouTubeSnippet,
    @SerializedName("contentDetails")
    val contentDetails: YouTubeContentDetails?
)

data class YouTubeContentDetails(
    @SerializedName("duration")
    val duration: String // ISO 8601 format (e.g., PT4M13S)
)

/**
 * Domain model for video items
 */
data class VideoItem(
    val id: String,
    val title: String,
    val description: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val publishedAt: String,
    val duration: String = "" // ISO 8601 duration format
)

