package com.example.notes

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.util.Date

class NoteDatabase(private val firestore: FirebaseFirestore) {

    fun getAllNotes(
        userId: String,
        onSuccess: (List<Note>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection("users")
            .document(userId)
            .collection("notes")
            .get()
            .addOnSuccessListener { snapshot ->
                val notes = snapshot.documents.map { doc ->
                    Note(
                        id = doc.id,
                        title = doc.getString("title") ?: "Untitled",
                        modifiedAt = doc.getTimestamp("modifiedAt")?.toDate() ?: Date()
                    )
                }
                onSuccess(notes)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun getNoteById(
        userId: String,
        noteId: String,
        onSuccess: (Note, List<Record>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val noteRef = firestore.collection("users")
            .document(userId)
            .collection("notes")
            .document(noteId)

        noteRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val note = Note(
                        id = document.id,
                        title = document.getString("title") ?: "Untitled",
                        modifiedAt = document.getTimestamp("modifiedAt")?.toDate() ?: Date()
                    )

                    noteRef.collection("records")
                        .get()
                        .addOnSuccessListener { recordsSnapshot ->
                            val records = parseRecords(recordsSnapshot)
                            onSuccess(note, records)
                        }
                        .addOnFailureListener { e -> onFailure(e) }
                } else {
                    onFailure(Exception("Note not found"))
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun saveNote(
        userId: String,
        note: Note,
        records: List<Record>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val noteRef = if (note.id.isNotEmpty()) {
            firestore.collection("users")
                .document(userId)
                .collection("notes")
                .document(note.id)
        } else {
            firestore.collection("users")
                .document(userId)
                .collection("notes")
                .document()
        }

        val noteData = mapOf(
            "title" to note.title,
            "modifiedAt" to FieldValue.serverTimestamp()
        )

        noteRef.set(noteData)
            .addOnSuccessListener {
                noteRef.collection("records").get()
                    .addOnSuccessListener { snapshot ->
                        snapshot.documents.forEach { it.reference.delete() }
                        records.forEachIndexed { index, record ->
                            val recordData = mapOf(
                                "content" to record.content,
                                "type" to record.type,
                                "is_checked" to (if (record.type == "checkbox") record.isChecked else null),
                                "order" to index,
                                "styles" to record.styles.map { it.name }
                            )
                            noteRef.collection("records").add(recordData)
                        }
                        onSuccess()
                    }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    fun deleteNote(
        userId: String,
        noteId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val noteRef = firestore.collection("users")
            .document(userId)
            .collection("notes")
            .document(noteId)

        noteRef.collection("records").get()
            .addOnSuccessListener { snapshot ->
                val deleteTasks = snapshot.documents.map { it.reference.delete() }
                Tasks.whenAll(deleteTasks)
                    .addOnSuccessListener {
                        noteRef.delete()
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { exception -> onFailure(exception) }
                    }
                    .addOnFailureListener { exception -> onFailure(exception) }
            }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    private fun parseRecords(snapshot: QuerySnapshot): List<Record> {
        return snapshot.documents.map { document ->
            Record(
                content = document.getString("content") ?: "",
                type = document.getString("type") ?: "text",
                isChecked = document.getBoolean("is_checked"),
                order = document.getLong("order")?.toInt() ?: 0,
                styles = (document.get("styles") as? List<String>)?.map { TextStyle.valueOf(it) } ?: emptyList()
            )
        }.sortedBy { it.order }
    }
}