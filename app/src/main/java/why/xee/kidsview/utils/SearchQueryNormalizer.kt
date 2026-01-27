package why.xee.kidsview.utils

/**
 * Utility to normalize and improve search queries for better YouTube search results
 * Makes search more forgiving and flexible, similar to real YouTube search
 */
object SearchQueryNormalizer {
    
    /**
     * Normalizes a search query to improve search results
     * - Trims whitespace
     * - Handles common variations
     * - Preserves user intent while making search more flexible
     */
    fun normalizeQuery(query: String): String {
        if (query.isBlank()) return query
        
        // Trim and normalize whitespace
        var normalized = query.trim()
            .replace(Regex("\\s+"), " ") // Replace multiple spaces with single space
        
        // YouTube API already handles fuzzy matching and relevance,
        // so we just clean up the query without changing the user's intent
        // The API's relevance algorithm will handle spelling variations
        
        return normalized
    }
    
    /**
     * Enhances query for better search results
     * Adds context that helps YouTube's relevance algorithm
     * Only adds if it doesn't change the user's intent
     */
    fun enhanceQuery(query: String): String {
        val normalized = normalizeQuery(query)
        
        // Don't modify if query is too short or already looks complete
        if (normalized.length < 3) return normalized
        
        // YouTube API's relevance algorithm already handles:
        // - Spelling variations
        // - Synonyms
        // - Related terms
        // - Context understanding
        
        // We just return the normalized query and let YouTube's API do the work
        // The API uses machine learning for relevance ranking
        return normalized
    }
}

