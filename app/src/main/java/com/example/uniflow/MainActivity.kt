package com.example.uniflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.example.uniflow.ui.theme.UniFlowTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.ZoneId



class MainActivity : ComponentActivity() {

    private var isDarkThemeState = mutableStateOf(false)

    override fun onResume() {
        super.onResume()
        // Ažurira temu pri povratku
        val prefs = getSharedPreferences("uniflow_prefs", MODE_PRIVATE)
        isDarkThemeState.value = prefs.getBoolean("dark_mode", false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("uniflow_prefs", MODE_PRIVATE)
        isDarkThemeState.value = prefs.getBoolean("dark_mode", false)

        val username = intent.getStringExtra("EXTRA_USERNAME") ?: "Student"

        setContent {
            UniFlowTheme(darkTheme = isDarkThemeState.value) {
                MainScreen(username = username, onSaveTask = { date, color, details, onComplete ->
                    saveTask(date, color, details, onComplete)
                })
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    username: String,
    onSaveTask: (LocalDate, Color, String, (Boolean) -> Unit) -> Unit
) {
    val taskList = remember { mutableStateListOf<Triple<LocalDate, Color, String>>() }
    var showDialog by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    FirebaseFirestore.getInstance()

    // dohvati zadatke iz firestorea prilikom prvog prikaza ekrana
    LaunchedEffect(Unit) {
        loadTasksForUser(
            onTasksLoaded = { tasks ->
                taskList.clear()
                taskList.addAll(tasks)
            },
            onError = {
                // dodaj kod za handlanje gresaka kasnije
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Izbornik", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                HorizontalDivider()

                NavigationDrawerItem(label = { Text("Kalendar") }, selected = false, onClick = {
                    scope.launch { drawerState.close() }
                })

                NavigationDrawerItem(label = { Text("Obaveze") }, selected = false, onClick = {
                    scope.launch { drawerState.close() }
                })

                NavigationDrawerItem(label = { Text("Postavke") }, selected = false, onClick = {
                    scope.launch { drawerState.close() }
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                })

                NavigationDrawerItem(label = { Text("Pomoć") }, selected = false, onClick = {})
                NavigationDrawerItem(label = { Text("O nama") }, selected = false, onClick = {})
            }
        },
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("UniFlow", fontWeight = FontWeight.Bold, color = Color.White)
                                Text(username, color = Color.White)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF31E981),
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White
                        )
                    )
                },
                floatingActionButton = { //  Ovdje je sad ispravno
                    FloatingActionButton(
                        onClick = { showDialog = true },
                        containerColor = Color(0xFF31E981),
                        contentColor = Color.White
                    ) {
                        Text("+")
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    FunctionalCalendar(taskList) { selectedDay = it }
                    Spacer(modifier = Modifier.height(16.dp))
                    selectedDay?.let { day ->
                        Text("Obaveze za ${formatDate(day)}:")
                        taskList.filter { it.first == day }.forEach { task ->
                            Text(task.third)
                        }
                    }
                }

                if (showDialog) {
                    AddTaskDialog(
                        onDismiss = { showDialog = false },
                        onSave = { date, color, details ->
                            // Pozovi spremanje u Firestore
                            onSaveTask(date, color, details) { success ->
                                if (success) {
                                    taskList.add(Triple(date, color, details))
                                } else {
                                    // možeš dodati Toast za grešku ovdje (ali u Compose treba Context)
                                }
                                showDialog = false
                            }
                        }
                    )
                }
            }
        }
    )
}

fun saveTask(date: LocalDate, color: Color, details: String, onComplete: (Boolean) -> Unit) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    if (userId == null) {
        onComplete(false)
        return
    }

    val db = FirebaseFirestore.getInstance()


    val task = hashMapOf(
        "date" to date.toString(),
        "color" to listOf(color.red, color.green, color.blue, color.alpha),
        "details" to details
    )

    db.collection("Users")
        .document(userId)
        .collection("Tasks")
        .add(task)
        .addOnSuccessListener {
            onComplete(true)
        }
        .addOnFailureListener {
            onComplete(false)
        }
}

fun loadTasksForUser(
    onTasksLoaded: (List<Triple<LocalDate, Color, String>>) -> Unit,
    onError: (Exception) -> Unit
) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    if (userId == null) {
        onTasksLoaded(emptyList())
        return
    }

    val db = FirebaseFirestore.getInstance()
    db.collection("Users")
        .document(userId)
        .collection("Tasks")
        .get()
        .addOnSuccessListener { result ->
            val tasks = mutableListOf<Triple<LocalDate, Color, String>>()
            for (document in result) {
                val dateStr = document.getString("date")
                val details = document.getString("details") ?: ""

                // dohvacanje boje obaveze iz firestorea, inace dolazi do crasha prilikom prijave
                val colorList = document.get("color") as? List<*>
                val color = if (colorList != null && colorList.size >= 4) {
                    Color(
                        red = (colorList[0] as Double).toFloat(),
                        green = (colorList[1] as Double).toFloat(),
                        blue = (colorList[2] as Double).toFloat(),
                        alpha = (colorList[3] as Double).toFloat()
                    )
                } else {
                    Color(0xFF31E981)
                }

                if (dateStr != null) {
                    val date = LocalDate.parse(dateStr)
                    tasks.add(Triple(date, color, details))
                }
            }
            onTasksLoaded(tasks)
        }
        .addOnFailureListener { exception ->
            onError(exception)
        }
}


@Composable
fun FunctionalCalendar(taskList: List<Triple<LocalDate, Color, String>>, onDateSelected: (LocalDate) -> Unit) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val markedDates = taskList.groupBy { it.first }.mapValues { it.value.first().second }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Text("<")
            }

            Text(
                "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale("hr"))} ${currentMonth.year}",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(8.dp)
            )

            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Text(">")
            }
        }

        val daysOfWeek = listOf("Pon", "Uto", "Sri", "Čet", "Pet", "Sub", "Ned")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            daysOfWeek.forEach { day ->
                Text(day, fontWeight = FontWeight.SemiBold)
            }
        }

        val firstDayOfMonth = (currentMonth.atDay(1).dayOfWeek.value % 7 + 6) % 7
        val daysInMonth = currentMonth.lengthOfMonth()

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(300.dp)
        ) {
            items(firstDayOfMonth) { Box(modifier = Modifier.size(40.dp)) }
            items((1..daysInMonth).toList()) { day ->
                val date = currentMonth.atDay(day)
                val color = markedDates[date] ?: Color.Transparent

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .padding(2.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(color)
                        .clickable { onDateSelected(date) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = day.toString(),
                        color = if (color != Color.Transparent) Color.White else Color.Unspecified)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onSave: (LocalDate, Color, String) -> Unit) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    )
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(Color(0xFF31E981)) }
    var taskType by remember { mutableStateOf("") }
    var taskName by remember { mutableStateOf("") }
    var taskTime by remember { mutableStateOf("") }

    val selectedDate: LocalDate? = datePickerState.selectedDateMillis?.let {
        java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj obavezu") },
        text = {
            Column {
                Text("Datum: ${selectedDate?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: "nije odabran"}")
                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = { showDatePicker = true }) {
                    Text("Odaberi datum")
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(taskType, { taskType = it }, label = { Text("Vrsta obaveze") })
                OutlinedTextField(taskName, { taskName = it }, label = { Text("Naziv obaveze") })
                OutlinedTextField(taskTime, { taskTime = it }, label = { Text("Vrijeme (opcionalno)") })

                Spacer(modifier = Modifier.height(16.dp))
                Text("Boja:")
                Row {
                    listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow).forEach { color ->
                        Box(
                            Modifier
                                .size(32.dp)
                                .padding(4.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(color)
                                .clickable { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (selectedDate != null && taskType.isNotBlank() && taskName.isNotBlank()) {
                    val details = "$taskType: $taskName ${if (taskTime.isNotBlank()) "- $taskTime" else ""}"
                    onSave(selectedDate, selectedColor, details)
                }
            }) {
                Text("Spremi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Odustani")
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Potvrdi")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
