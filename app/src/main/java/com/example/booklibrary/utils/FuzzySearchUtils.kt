package com.example.booklibrary.utils

import java.util.Locale

object FuzzySearchUtils {

    fun levenshteinDistance(s1: String, s2: String): Int {
        val str1 = s1.lowercase(Locale.getDefault())
        val str2 = s2.lowercase(Locale.getDefault())

        val dp = Array(str1.length + 1) { IntArray(str2.length + 1) }

        for (i in 0..str1.length) dp[i][0] = i
        for (j in 0..str2.length) dp[0][j] = j

        for (i in 1..str1.length) {
            for (j in 1..str2.length) {
                val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[str1.length][str2.length]
    }

    fun isMatch(source: String?, query: String, maxDistance: Int = 1): Boolean {
        if (query.isBlank()) return true
        if (source == null) return false

        val srcLower = source.lowercase(Locale.getDefault())
        val qLower = query.lowercase(Locale.getDefault()).trim()

        if (srcLower.contains(qLower)) return true

        val sourceWords = srcLower.split(" ", "-", ",", ".").filter { it.length > 2 }
        val queryWords = qLower.split(" ").filter { it.length > 2 }

        if (queryWords.isEmpty()) return false

        return queryWords.all { qWord ->
            sourceWords.any { sWord ->
                val threshold = if (qWord.length <= 4) 1 else 2
                levenshteinDistance(sWord, qWord) <= threshold
            }
        }
    }
}