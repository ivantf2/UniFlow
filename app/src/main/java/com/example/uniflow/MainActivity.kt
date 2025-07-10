package com.example.uniflow

import android.annotation.SuppressLint
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
import androidx.compose.ui.platform.LocalContext
import com.example.uniflow.ui.theme.UniFlowTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.ZoneId
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.core.content.edit


@Suppress("NAME_SHADOWING")
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
                MainScreen(
                    username = username,
                    isDarkMode = isDarkThemeState.value,
                    onToggleDarkMode = { enabled ->
                        isDarkThemeState.value = enabled
                        val prefs = getSharedPreferences("uniflow_prefs", MODE_PRIVATE)
                        prefs.edit { putBoolean("dark_mode", enabled) }
                    },
                    onSaveTask = { date, color, details, onComplete ->
                    saveTask(date, color, details, onComplete)
                })
            }
        }

    }
}

enum class Screen {
    Calendar, Tasks, Settings, Help, About
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    username: String,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean)-> Unit,
    onSaveTask: (LocalDate, Color, String, (Boolean) -> Unit) -> Unit
) {
    val taskList = remember { mutableStateListOf<Triple<LocalDate, Color, String>>() }
    var showDialog by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    LocalContext.current
    FirebaseFirestore.getInstance()

    var currentScreen by remember { mutableStateOf(Screen.Calendar) }


    // dohvati obaveze (tasks u firestore) iz firestorea prilikom prvog prikaza ekrana
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

                NavigationDrawerItem(label = { Text("Kalendar") }, selected = currentScreen == Screen.Calendar, onClick = {
                    currentScreen = Screen.Calendar
                    scope.launch { drawerState.close() }
                })


                NavigationDrawerItem(
                    label = { Text("Obaveze") },
                    selected = currentScreen == Screen.Tasks,
                    onClick = {
                        currentScreen = Screen.Tasks
                        scope.launch { drawerState.close() }
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Postavke") },
                    selected = currentScreen == Screen.Settings,
                    onClick = {
                        currentScreen = Screen.Settings
                        scope.launch { drawerState.close() }
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Pomoć") },
                    selected = currentScreen == Screen.Help,
                    onClick = {
                        currentScreen = Screen.Help
                        scope.launch { drawerState.close() }
                    }
                )

                NavigationDrawerItem(
                    label = { Text("O nama") },
                    selected = currentScreen == Screen.About,
                    onClick = {
                        currentScreen = Screen.About
                        scope.launch { drawerState.close() }
                    }
                )
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
                floatingActionButton = {
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


                    when (currentScreen) {
                        Screen.Calendar -> {
                            FunctionalCalendar(taskList) { selectedDay = it }
                            Spacer(modifier = Modifier.height(16.dp))
                            selectedDay?.let { day ->
                                Text("Obaveze za ${formatDate(day)}:")
                                taskList.filter { it.first == day }.forEach { task ->
                                    Text(task.third)
                                }
                            }
                        }

                        Screen.Tasks -> {
                            Text("Sve obaveze:", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            taskList.sortedBy { it.first }.forEach { task ->
                                Column(modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)) {
                                    Text(
                                        text = formatDate(task.first),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = task.third,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                        Screen.Settings -> {
                            SettingsContent(isDarkMode = isDarkMode, onToggleDarkMode = onToggleDarkMode)
                        }
                        Screen.Help -> {
                            PomocScreen()
                        }
                        Screen.About -> {
                            ONamaScreen()
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
                                    // handlanje gresaka
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
fun SettingsContent(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Postavke", fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tamni način rada")
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isDarkMode,
                onCheckedChange = onToggleDarkMode
            )
        }
    }
}

@SuppressLint("QueryPermissionsNeeded")
fun sendEmail(context: Context, to: String, subject: String, body: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "message/rfc822"
        putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }

        // dijalog gdje korisnik treba odabrat email aplikaciju kojom se salje mail
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(Intent.createChooser(intent, "Pošalji email putem..."))
    } else {
        Toast.makeText(context, "Nema instalirane email aplikacije", Toast.LENGTH_LONG).show()
    }
}


@Composable
fun KontaktForma() {
    val context = LocalContext.current

    var ime by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var poruka by remember { mutableStateOf("") }

    Column {
        Text("Kontaktirajte nas", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = ime,
            onValueChange = { ime = it },
            label = { Text("Vaše ime") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email adresa") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = poruka,
            onValueChange = { poruka = it },
            label = { Text("Poruka") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val naslov = "Kontakt poruka od $ime"
            val sadrzaj = "Ime: $ime\nEmail: $email\n\nPoruka:\n$poruka"
            sendEmail(
                context = context,
                to = "ivan.pribanic04@gmail.com",
                subject = naslov,
                body = sadrzaj
            )
        }) {
            Text("Pošalji")
        }
    }
}




@Composable
fun PomocScreen() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text("Pomoć", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Upute za korištenje aplikacije:", fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))
        Text("• Dodavanje zadatka: Na glavnom ekranu kliknite '+' kako biste unijeli novi zadatak.")
        Text("• Kalendar: Prikazuje sve vaše zadatke po datumima.")
        Text("• Obaveze: Kronološki popis obaveza kako nebi zaboravili aktivnosti.")
        Text("• Dark mode: Dostupan u postavkama aplikacije.")
        Text("• Sinkronizacija: Vaši podaci su spremljeni putem Firestore baze podataka na Firebase-u.")

        Spacer(modifier = Modifier.height(16.dp))
        Text("Ako imate dodatna pitanja, obratite se putem kontakt forme.")
        Spacer(modifier = Modifier.height(24.dp))
        KontaktForma()
    }
}

@Composable
fun ONamaScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("O nama", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            """
            Ova aplikacija rezultat je suradnje između Ivana Pribanića i Karla Lausa na projektnim zadacima tijekom kolegija.
            
            Cilj aplikacije je pomoći studentima u organizaciji dnevnih i tjednih obaveza, uključujući predavanja, ispite i zadatke.
            
            Razvoj aplikacije nastavljen je od strane Ivana Pribanića kao dio završnog rada.
            """.trimIndent()
        )
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
