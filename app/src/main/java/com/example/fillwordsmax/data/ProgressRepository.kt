package com.example.fillwordsmax.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

object ProgressRepository {
    private val db = FirebaseFirestore.getInstance()
    private val userId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    fun saveLevelProgress(levelId: Int, isCompleted: Boolean, isLocked: Boolean, score: Int, time: Long, stars: Int) {
        userId?.let { uid ->
            val data = hashMapOf(
                "isCompleted" to isCompleted,
                "isLocked" to isLocked,
                "score" to score,
                "time" to time,
                "stars" to stars
            )
            db.collection("users")
                .document(uid)
                .collection("progress")
                .document(levelId.toString())
                .set(data)
        }
    }

    fun loadProgress(
        onResult: (Map<Int, ProgressData>) -> Unit
    ) {
        userId?.let { uid ->
            db.collection("users")
                .document(uid)
                .collection("progress")
                .get()
                .addOnSuccessListener { result ->
                    val progress = mutableMapOf<Int, ProgressData>()
                    for (doc in result) {
                        val levelId = doc.id.toIntOrNull() ?: continue
                        val isCompleted = doc.getBoolean("isCompleted") ?: false
                        val isLocked = doc.getBoolean("isLocked") ?: true
                        val score = doc.getLong("score")?.toInt() ?: 0
                        val time = doc.getLong("time") ?: 0L
                        val stars = doc.getLong("stars")?.toInt() ?: 0
                        progress[levelId] = ProgressData(isCompleted, isLocked, score, time, stars)
                    }
                    onResult(progress)
                }
                .addOnFailureListener {
                    onResult(emptyMap())
                }
        } ?: onResult(emptyMap())
    }
}

data class ProgressData(
    val isCompleted: Boolean,
    val isLocked: Boolean,
    val score: Int,
    val time: Long,
    val stars: Int
) 