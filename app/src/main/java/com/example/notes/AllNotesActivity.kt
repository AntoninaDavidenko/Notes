package com.example.notes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource

class AllNotesActivity : ComponentActivity() {
    private val noteDatabase = NoteDatabase(FirebaseFirestore.getInstance())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NotesScreen(
                onLogout = { logout() },
                onCreateNote = { navigateToNoteEditActivity() },
                onViewNote = { noteId -> navigateToNoteViewActivity(noteId) },
                noteDatabase = noteDatabase
            )
        }
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToNoteViewActivity(noteId: String) {
        val intent = Intent(this, NoteViewActivity::class.java)
        intent.putExtra("NOTE_ID", noteId)
        startActivity(intent)
    }

    private fun navigateToNoteEditActivity(noteId: String? = null) {
        val intent = Intent(this, NoteEditActivity::class.java)
        noteId?.let { intent.putExtra("NOTE_ID", it) }
        startActivity(intent)
    }
}

@Composable
fun NotesScreen(
    onLogout: () -> Unit,
    onCreateNote: () -> Unit,
    onViewNote: (String) -> Unit,
    noteDatabase: NoteDatabase
) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val notes = remember { mutableStateListOf<Note>() }


    LaunchedEffect(userId) {
        if (userId != null) {
            noteDatabase.getAllNotes(
                userId = userId,
                onSuccess = { fetchedNotes ->
                    notes.clear()
                    notes.addAll(fetchedNotes.sortedByDescending { it.modifiedAt })
                },
                onFailure = { e -> e.printStackTrace() }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Нотатки") },
                backgroundColor = Color(0xFF246156),
                contentColor = Color.White,
                actions = {
                    TextButton(onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF246156), contentColor = Color.White),) {
                        Text("Вихід", color = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateNote, backgroundColor = Color(0xFF246156), contentColor = Color.White) {
                Icon(Icons.Filled.Add, contentDescription = "Add Note")
            }
        },
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (notes.isEmpty()) {
                    Image(
                        painter = painterResource(id = R.drawable.frame),
                        contentDescription = "No notes",
                        modifier = Modifier

                            .align(Alignment.Center)
                    )
                } else {
                    NotesGrid(notes = notes, onNoteClick = onViewNote)
                }
            }
        }
    )
}

@Composable
fun NotesGrid(notes: List<Note>, onNoteClick: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(notes) { note ->
            NoteCard(note = note, onNoteClick = onNoteClick)
        }
    }
}

@Composable
fun NoteCard(note: Note, onNoteClick: (String) -> Unit) {
    var records by remember { mutableStateOf<List<Record>>(emptyList()) }
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(note.id) {
        if (userId != null) {
            firestore.collection("users")
                .document(userId)
                .collection("notes")
                .document(note.id)
                .collection("records")
                .get()
                .addOnSuccessListener { snapshot ->
                    records = snapshot.documents.map { doc ->
                        Record(
                            content = doc.getString("content") ?: "",
                            type = doc.getString("type") ?: "text",
                            isChecked = doc.getBoolean("is_checked"),
                            order = doc.getLong("order")?.toInt() ?: 0,
                            styles = (doc.get("styles") as? List<String>)?.map { TextStyle.valueOf(it) } ?: emptyList()
                        )
                    }.sortedBy { it.order }
                }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable { onNoteClick(note.id) },
        elevation = 4.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxSize()
        ) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = records.joinToString(separator = "\n") { record ->
                     record.content
                },
                style = MaterialTheme.typography.body2,
                maxLines = 3, // Обмеження в 3 рядки
                overflow = TextOverflow.Ellipsis, // Якщо більше рядків заміть них додається ...
                modifier = Modifier.weight(1f)
            )

            Text(
                text = formatDate(note.modifiedAt),
                style = MaterialTheme.typography.caption,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("HH:mm dd.MM.yyyy", Locale.getDefault())
    return formatter.format(date)
}


