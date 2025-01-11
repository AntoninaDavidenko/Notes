package com.example.notes

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NoteViewActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val noteDatabase = NoteDatabase(FirebaseFirestore.getInstance())
    private var noteId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Получаем noteId для просмотра
        noteId = intent.getStringExtra("NOTE_ID")

        setContent {
            NoteViewScreen(
                noteId = noteId,
                onLoadNote = { onNoteLoaded ->
                    loadNoteFromDatabase(onNoteLoaded)
                },
                onDeleteNote = {
                    deleteNoteFromDatabase()
                },
                onEditNote = {
                    startActivity(
                        Intent(this, NoteEditActivity::class.java).apply {
                            putExtra("NOTE_ID", noteId)
                        }
                    )
                }
            )
        }
    }

    private fun loadNoteFromDatabase(onNoteLoaded: (Note, List<Record>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null || noteId == null) {
            return
        }

        noteDatabase.getNoteById(
            userId = userId,
            noteId = noteId!!,
            onSuccess = { note, records ->
                onNoteLoaded(note, records)
            },
            onFailure = { exception ->
                runOnUiThread {
                    Toast.makeText(this, "Error loading note: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun deleteNoteFromDatabase() {
        val userId = auth.currentUser?.uid
        if (userId == null || noteId == null) {
            Toast.makeText(this, "Cannot delete note", Toast.LENGTH_SHORT).show()
            return
        }

        noteDatabase.deleteNote(
            userId = userId,
            noteId = noteId!!,
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "Note deleted successfully", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, AllNotesActivity::class.java))
                    finish()
                }
            },
            onFailure = { exception ->
                runOnUiThread {
                    Toast.makeText(this, "Error deleting note: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun NoteViewScreen(
    noteId: String?,
    onLoadNote: ((Note, List<Record>) -> Unit) -> Unit,
    onDeleteNote: () -> Unit,
    onEditNote: () -> Unit
) {
    val context = LocalContext.current
    var titleState by remember { mutableStateOf("") }
    var modifiedAtState by remember { mutableStateOf("") }
    var records by remember { mutableStateOf(listOf<Record>()) }

    LaunchedEffect(noteId) {
        if (noteId != null) {
            onLoadNote { note, loadedRecords ->
                titleState = note.title
                modifiedAtState = formatDate(note.modifiedAt)
                records = loadedRecords
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("View Note") },
                backgroundColor = Color(0xFF246156),
                contentColor = Color.White,
                navigationIcon = {
                    IconButton(onClick = {
                        context.startActivity(Intent(context, AllNotesActivity::class.java))
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onDeleteNote) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete Note")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onEditNote, backgroundColor = Color(0xFF246156), contentColor = Color.White) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit Note")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Заголовок и дата
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp) // Отступ, чтобы выровнять текст
            ) {
                Text(
                    text = titleState,
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier.padding(bottom = 8.dp) // Отступ между заголовком и датой
                )

                Text(
                    text = modifiedAtState,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            LazyColumn {
                items(records) { record ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp) // Единый отступ слева
                    ) {
                        if (record.type == "checkbox") {
                            Checkbox(
                                checked = record.isChecked == true,
                                onCheckedChange = null, // Чекбоксы только для просмотра
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF246156),  // Цвет для отмеченного состояния
                                    uncheckedColor = Color.Gray,      // Цвет для неотмеченного состояния
                                    checkmarkColor = Color.White      // Цвет галочки
                                ),
                                modifier = Modifier.padding(end = 8.dp) // Отступ между чекбоксом и текстом
                            )
                        }
                        Text(
                            text = record.content,
                            style = MaterialTheme.typography.body1,
                            fontWeight = if (record.styles.contains(TextStyle.BOLD)) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (record.styles.contains(TextStyle.ITALIC)) FontStyle.Italic else FontStyle.Normal,
                            textDecoration = when {
                                record.styles.contains(TextStyle.UNDERLINE) && record.styles.contains(TextStyle.STRIKETHROUGH) ->
                                    TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                                record.styles.contains(TextStyle.UNDERLINE) -> TextDecoration.Underline
                                record.styles.contains(TextStyle.STRIKETHROUGH) -> TextDecoration.LineThrough
                                else -> TextDecoration.None
                            },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }

        }
    }
}
