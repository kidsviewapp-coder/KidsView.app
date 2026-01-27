package why.xee.kidsview.utils

/**
 * Utility functions for parsing and formatting YouTube video durations
 * YouTube API returns durations in ISO 8601 format (e.g., PT4M13S = 4 minutes 13 seconds)
 */
object DurationUtils {
    
    /**
     * Parse ISO 8601 duration format to human-readable string
     * Examples:
     * - PT4M13S -> 4:13
     * - PT1H2M30S -> 1:02:30
     * - PT45S -> 0:45
     */
    fun parseISO8601Duration(isoDuration: String): String {
        if (isoDuration.isEmpty()) return ""
        
        try {
            // Remove PT prefix
            var duration = isoDuration.replace("PT", "")
            
            var hours = 0
            var minutes = 0
            var seconds = 0
            
            // Extract hours
            if (duration.contains("H")) {
                val hoursIndex = duration.indexOf("H")
                hours = duration.substring(0, hoursIndex).toInt()
                duration = duration.substring(hoursIndex + 1)
            }
            
            // Extract minutes
            if (duration.contains("M")) {
                val minutesIndex = duration.indexOf("M")
                minutes = duration.substring(0, minutesIndex).toInt()
                duration = duration.substring(minutesIndex + 1)
            }
            
            // Extract seconds
            if (duration.contains("S")) {
                val secondsIndex = duration.indexOf("S")
                seconds = duration.substring(0, secondsIndex).toInt()
            }
            
            return when {
                hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
                minutes > 0 -> String.format("%d:%02d", minutes, seconds)
                else -> String.format("0:%02d", seconds)
            }
        } catch (e: Exception) {
            return ""
        }
    }
}

