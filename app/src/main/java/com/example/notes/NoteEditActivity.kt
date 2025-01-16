package com.example.notes

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


import android.view.WindowManager
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding

class NoteEditActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val noteDatabase = NoteDatabase(FirebaseFirestore.getInstance())
    private var noteId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        noteId = intent.getStringExtra("NOTE_ID")

        setContent {
            NoteScreen(
                noteId = noteId,
                onNoteSaved = { title, records ->
                    saveNoteToDatabase(title, records)
                },
                onLoadNote = { onNoteLoaded ->
                    loadNoteFromDatabase(onNoteLoaded)
                }
            )
        }
    }

    private fun saveNoteToDatabase(title: String, records: List<Record>) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Користувач неавторизований", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this, "Нотатка успішно збережена", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, NoteViewActivity::class.java).apply {
                        putExtra("NOTE_ID", note.id)
                    }
                    startActivity(intent)
                    finish()
                }
            },
            onFailure = { exception ->
                runOnUiThread {
                    Toast.makeText(this, "Не вдалося зберегти нотатку: ${exception.message}", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this, "Не вдалося завантажити нотатку: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun NoteScreen(
    noteId: String?,
    onNoteSaved: (String, List<Record>) -> Unit,
    onLoadNote: ((Note, List<Record>) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var titleState by remember { mutableStateOf("") }
    var records by remember { mutableStateOf(mutableListOf<Record>()) }
    var currentRecordText by remember { mutableStateOf("") }
    var currentStyles by remember { mutableStateOf(setOf<TextStyle>()) }
    var isCheckboxMode by remember { mutableStateOf(false) }
    var editingRecordIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(noteId) {
        if (noteId != null) {
            onLoadNote { note, loadedRecords ->
                titleState = note.title
                records = loadedRecords.toMutableList()
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .navigationBarsPadding()
            .statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(if (noteId != null) "Редагування нотатки" else "Створення нотатки") },
                backgroundColor = Color(0xFF246156),
                contentColor = Color.White,
                navigationIcon = {
                    IconButton(onClick = {
                        if (noteId == null) {
                            context.startActivity(Intent(context, AllNotesActivity::class.java))
                        } else {
                            context.startActivity(Intent(context, NoteViewActivity::class.java).apply {
                                putExtra("NOTE_ID", noteId)
                            })
                        }
                        (context as ComponentActivity).finish()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (titleState.isNotBlank()) {
                            onNoteSaved(titleState, records)
                        } else {
                            Toast.makeText(
                                context,
                                "Заголовок не може бути порожнім",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save Note")
                    }
                }
            )
        },
        bottomBar = @androidx.compose.runtime.Composable {
            BottomAppBar(
                modifier = Modifier.imePadding(),
                backgroundColor = Color.White,
                contentColor = Color(0xFF246156)
            ) {
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
                            tint = if (currentStyles.contains(TextStyle.BOLD)) Color(0xFF246156) else Color.Gray
                        )
                    }
                    IconButton(
                        onClick = { currentStyles = currentStyles.toggle(TextStyle.ITALIC) }
                    ) {
                        Icon(
                            Icons.Filled.FormatItalic,
                            contentDescription = "Italic",
                            tint = if (currentStyles.contains(TextStyle.ITALIC)) Color(0xFF246156) else Color.Gray
                        )
                    }
                    IconButton(
                        onClick = { currentStyles = currentStyles.toggle(TextStyle.UNDERLINE) }
                    ) {
                        Icon(
                            Icons.Filled.FormatUnderlined,
                            contentDescription = "Underline",
                            tint = if (currentStyles.contains(TextStyle.UNDERLINE)) Color(0xFF246156) else Color.Gray
                        )
                    }
                    IconButton(
                        onClick = { currentStyles = currentStyles.toggle(TextStyle.STRIKETHROUGH) }
                    ) {
                        Icon(
                            Icons.Filled.StrikethroughS,
                            contentDescription = "Strikethrough",
                            tint = if (currentStyles.contains(TextStyle.STRIKETHROUGH)) Color(
                                0xFF246156
                            ) else Color.Gray
                        )
                    }
                    IconButton(
                        onClick = { isCheckboxMode = !isCheckboxMode }
                    ) {
                        Icon(
                            imageVector = if (isCheckboxMode) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                            contentDescription = "Checkbox Mode",
                            tint = if (isCheckboxMode) Color(0xFF246156) else Color.Gray
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            TextField(
                value = titleState,
                onValueChange = { titleState = it },
                label = { Text("Заголовок") },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color.Transparent, shape = RoundedCornerShape(8.dp)),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFF246156),
                    unfocusedIndicatorColor = Color.Gray,
                    cursorColor = Color(0xFF246156),
                    textColor = Color.Black,
                    focusedLabelColor = Color(0xFF246156),
                    unfocusedLabelColor = Color.Gray
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = currentRecordText,
                onValueChange = { currentRecordText = it },
                label = { Text("Текст") },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color.Transparent, shape = RoundedCornerShape(8.dp)),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFF246156),
                    unfocusedIndicatorColor = Color.Gray,
                    cursorColor = Color(0xFF246156),
                    textColor = Color.Black,
                    focusedLabelColor = Color(0xFF246156),
                    unfocusedLabelColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        if (currentRecordText.isNotBlank()) {
                            val newRecord = Record(
                                content = currentRecordText,
                                type = if (isCheckboxMode) "checkbox" else "text",
                                isChecked = if (isCheckboxMode) false else null,
                                styles = currentStyles.toList()
                            )
                            if (editingRecordIndex == null) {
                                records.add(newRecord)
                            } else {
                                if (records[editingRecordIndex!!].isChecked != null)
                                    newRecord.isChecked = records[editingRecordIndex!!].isChecked
                                records[editingRecordIndex!!] = newRecord
                                editingRecordIndex = null
                            }
                            currentRecordText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF246156), contentColor = Color.White),
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier
                        .width(240.dp)
                        .height(56.dp)
                ) {
                    Text(if (editingRecordIndex == null) "Додати запис" else "Оновити запис", fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                records.forEachIndexed { index, record ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (record.type == "checkbox") {
                            record.isChecked?.let { isChecked ->
                                val isCheckedState =
                                    remember { mutableStateOf(record.isChecked ?: false) }
                                Checkbox(
                                    checked = isCheckedState.value,
                                    onCheckedChange = { isCheckedNew ->
                                        isCheckedState.value = isCheckedNew
                                        record.isChecked = isCheckedNew
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF246156),
                                        uncheckedColor = Color.Gray,
                                        checkmarkColor = Color.White
                                    )
                                )
                            }
                        }
                        Text(
                            text = record.content,
                            style = MaterialTheme.typography.body1,
                            fontWeight = if (record.styles.contains(TextStyle.BOLD)) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (record.styles.contains(TextStyle.ITALIC)) FontStyle.Italic else FontStyle.Normal,
                            textDecoration = when {
                                record.styles.contains(TextStyle.UNDERLINE) && record.styles.contains(TextStyle.STRIKETHROUGH) ->
                                    TextDecoration.combine(
                                        listOf(
                                            TextDecoration.Underline,
                                            TextDecoration.LineThrough
                                        )
                                    )

                                record.styles.contains(TextStyle.UNDERLINE) -> TextDecoration.Underline
                                record.styles.contains(TextStyle.STRIKETHROUGH) -> TextDecoration.LineThrough
                                else -> TextDecoration.None
                            },
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .weight(1f)
                                .clickable {
                                    currentRecordText = record.content
                                    isCheckboxMode = record.type == "checkbox"
                                    currentStyles = record.styles.toSet()
                                    editingRecordIndex = index
                                }
                        )

                        IconButton(
                            onClick = {
                                records = records.toMutableList().apply { removeAt(index) }
                            }
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete Record")
                        }
                    }
                }
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
