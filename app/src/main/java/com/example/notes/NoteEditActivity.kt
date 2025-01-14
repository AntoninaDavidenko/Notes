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
import kotlinx.coroutines.launch

import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding

class NoteEditActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val noteDatabase = NoteDatabase(FirebaseFirestore.getInstance())
    private var noteId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure window to handle IME (Input Method Editor)
        //WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

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
                    val intent = Intent(this, NoteViewActivity::class.java).apply {
                        putExtra("NOTE_ID", note.id)
                    }
                    startActivity(intent)
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
    var editingRecordIndex by remember { mutableStateOf<Int?>(null) }

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
        modifier = Modifier
            .navigationBarsPadding() // Для учета нижних жестов/панели
            .statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(if (noteId != null) "Edit Note" else "Create Note") },
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
                                "Title cannot be empty",
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
            // Поле для заголовка
            TextField(
                value = titleState,
                onValueChange = { titleState = it },
                label = { Text("Title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color.Transparent, shape = RoundedCornerShape(8.dp)),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFF246156),
                    unfocusedIndicatorColor = Color.Gray,
                    cursorColor = Color(0xFF246156),
                    textColor = Color.Black,
                    focusedLabelColor = Color(0xFF246156), // Цвет текста label при фокусе
                    unfocusedLabelColor = Color.Gray // Цвет текста label без фокуса
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Поле для контента
            TextField(
                value = currentRecordText,
                onValueChange = { currentRecordText = it },
                label = { Text("Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color.Transparent, shape = RoundedCornerShape(8.dp)),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFF246156),
                    unfocusedIndicatorColor = Color.Gray,
                    cursorColor = Color(0xFF246156),
                    textColor = Color.Black,
                    focusedLabelColor = Color(0xFF246156), // Цвет текста label при фокусе
                    unfocusedLabelColor = Color.Gray // Цвет текста label без фокуса
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка добавления записи
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center // Keeps the button centered
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
                        .width(240.dp) // Уменьшена ширина кнопки
                        .height(56.dp) // Высота кнопки
                ) {
                    Text(if (editingRecordIndex == null) "Add Record" else "Update Record", fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Прокручиваемая область для записей
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Используем вес, чтобы область занимала оставшееся пространство
                    .verticalScroll(rememberScrollState()) // Добавляем прокрутку
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
                                        checkedColor = Color(0xFF246156),  // Цвет для отмеченного состояния
                                        uncheckedColor = Color.Gray,      // Цвет для неотмеченного состояния
                                        checkmarkColor = Color.White      // Цвет галочки
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
