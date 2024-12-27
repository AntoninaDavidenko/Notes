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
import kotlinx.coroutines.launch

class NoteActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val noteDatabase = NoteDatabase(FirebaseFirestore.getInstance())
    private var noteId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Получаем noteId, если редактируем заметку
        noteId = intent.getStringExtra("NOTE_ID")

        setContent {
            NoteScreen(
                noteId = noteId,
                onNoteSaved = { title, records ->
                    saveNoteToDatabase(title, records)
                },
                onLoadNote = { onNoteLoaded ->
                    loadNoteFromDatabase(onNoteLoaded)
                },
                onDeleteNote = {
                    deleteNoteFromDatabase()
                }
            )
        }
    }


    private fun saveNoteToDatabase(title: String, records: List<Record>) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val note = Note(
            id = noteId ?: FirebaseFirestore.getInstance().collection("notes").document().id,
            title = title
        )

        noteDatabase.saveNote(
            userId = userId,
            note = note,
            records = records,
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "Note saved successfully", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, NotesActivity::class.java))
                    finish()
                }
            },
            onFailure = { exception ->
                runOnUiThread {
                    Toast.makeText(this, "Error saving note: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
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
                    startActivity(Intent(this, NotesActivity::class.java))
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
fun NoteScreen(
    noteId: String?,
    onNoteSaved: (String, List<Record>) -> Unit,
    onLoadNote: ((Note, List<Record>) -> Unit) -> Unit,
    onDeleteNote: () -> Unit
) {
    val context = LocalContext.current
    var titleState by remember { mutableStateOf("") }
    var records by remember { mutableStateOf(mutableListOf<Record>()) }
    var currentRecordText by remember { mutableStateOf("") }
    var currentStyles by remember { mutableStateOf(setOf<TextStyle>()) }
    var isCheckboxMode by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(noteId) {
        if (noteId != null) {
            onLoadNote { note, loadedRecords ->
                titleState = note.title
                records = loadedRecords.toMutableList()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId != null) "Edit Note" else "Create Note") },
                actions = {
                    if (noteId != null) {
                        IconButton(onClick = onDeleteNote) {
                            Icon(Icons.Filled.Delete, "Delete Note")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            TextField(
                value = titleState,
                onValueChange = { titleState = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = currentRecordText,
                onValueChange = { currentRecordText = it },
                label = { Text("Content") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = { currentStyles = currentStyles.toggle(TextStyle.BOLD) }
                ) {
                    Icon(
                        Icons.Filled.FormatBold,
                        contentDescription = "Bold",
                        tint = if (currentStyles.contains(TextStyle.BOLD)) MaterialTheme.colors.primary else Color.Gray
                    )
                }
                IconButton(
                    onClick = { currentStyles = currentStyles.toggle(TextStyle.ITALIC) }
                ) {
                    Icon(
                        Icons.Filled.FormatItalic,
                        contentDescription = "Italic",
                        tint = if (currentStyles.contains(TextStyle.ITALIC)) MaterialTheme.colors.primary else Color.Gray
                    )
                }
                IconButton(
                    onClick = { currentStyles = currentStyles.toggle(TextStyle.UNDERLINE) }
                ) {
                    Icon(
                        Icons.Filled.FormatUnderlined,
                        contentDescription = "Underline",
                        tint = if (currentStyles.contains(TextStyle.UNDERLINE)) MaterialTheme.colors.primary else Color.Gray
                    )
                }
                IconButton(
                    onClick = { currentStyles = currentStyles.toggle(TextStyle.STRIKETHROUGH) }
                ) {
                    Icon(
                        Icons.Filled.StrikethroughS,
                        contentDescription = "Strikethrough",
                        tint = if (currentStyles.contains(TextStyle.STRIKETHROUGH)) MaterialTheme.colors.primary else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        if (currentRecordText.isNotBlank()) {
                            records.add(
                                Record(
                                    content = currentRecordText,
                                    type = if (isCheckboxMode) "checkbox" else "text",
                                    styles = currentStyles.toList()
                                )
                            )
                            currentRecordText = ""
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add Record")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { isCheckboxMode = !isCheckboxMode },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isCheckboxMode) "Switch to Text" else "Switch to Checkbox")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (titleState.isNotBlank()) {
                        coroutineScope.launch {
                            onNoteSaved(titleState, records)
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Title cannot be empty",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Note")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Records:", style = MaterialTheme.typography.h6)
            records.forEach { record ->
                    Text(
                        text = if (record.type == "checkbox") "[ ] ${record.content}" else record.content,
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
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
            }

        }
    }
}

private fun Set<TextStyle>.toggle(style: TextStyle): Set<TextStyle> {
    return if (contains(style)) {
        minus(style)
    } else {
        plus(style)
    }
}