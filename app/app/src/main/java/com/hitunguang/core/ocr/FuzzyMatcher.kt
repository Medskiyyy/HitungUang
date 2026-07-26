package com.hitunguang.core.ocr

/**
 * Simple edit-distance fuzzy matcher for OCR typo robustness.
 * Lightweight alternative to external fuzzywuzzy library.
 */
object FuzzyMatcher {

    private const val DEFAULT_THRESHOLD = 2

    /**
     * Returns true if [text] matches any keyword in [keywords] within [maxDistance] edits.
     */
    fun matchesAny(text: String, keywords: List<String>, maxDistance: Int = DEFAULT_THRESHOLD): Boolean {
        val lower = text.lowercase()
        return keywords.any { keyword ->
            levenshtein(lower, keyword.lowercase()) <= maxDistance
        }
    }

    /**
     * Find best matching keyword from [keywords], or null if none within threshold.
     */
    fun findBestMatch(text: String, keywords: List<String>, maxDistance: Int = DEFAULT_THRESHOLD): String? {
        val lower = text.lowercase()
        return keywords
            .map { it to levenshtein(lower, it.lowercase()) }
            .filter { it.second <= maxDistance }
            .minByOrNull { it.second }
            ?.first
    }

    /** Classic Wagner-Fischer Levenshtein distance. */
    private fun levenshtein(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val costs = IntArray(b.length + 1)
        for (j in costs.indices) costs[j] = j
        for (i in 1..a.length) {
            var prev = costs[0]
            costs[0] = i
            for (j in 1..b.length) {
                val temp = costs[j]
                costs[j] = if (a[i - 1] == b[j - 1]) {
                    prev
                } else {
                    1 + minOf(prev, costs[j], costs[j - 1])
                }
                prev = temp
            }
        }
        return costs[b.length]
    }
}
