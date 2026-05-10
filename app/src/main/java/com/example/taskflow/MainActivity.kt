package com.example.taskflow


import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.taskflow.ui.theme.TaskFlowTheme

val PrimaryBlue = Color(0xFF2564CF)
val LightBlue = Color(0xFFEFF4FF)
val HighColor = Color(0xFFD32F2F)
val MediumColor = Color(0xFFF57C00)
val LowColor = Color(0xFF388E3C)
val BgColor = Color(0xFFF5F5F5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaskFlowTheme {
                val db = Room.databaseBuilder(
                    applicationContext,
                    TaskDatabase::class.java,
                    "task_db"
                ).fallbackToDestructiveMigration().build()

                val viewModel: TaskViewModel = ViewModelProvider(
                    this@MainActivity,
                    TaskViewModelFactory(db.taskDao())
                ).get(TaskViewModel::class.java)

                TaskApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskApp(viewModel: TaskViewModel) {
    val context = LocalContext.current
    val tasks by viewModel.filteredTasks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Todas") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Tareas", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue),
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, null, tint = Color.White)
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            listOf("nombre", "fecha", "estado", "prioridad").forEach { opcion ->
                                DropdownMenuItem(
                                    text = { Text("Por $opcion") },
                                    onClick = { viewModel.setSortOrder(opcion); showSortMenu = false }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = PrimaryBlue) {
                Icon(Icons.Default.Add, null, tint = Color.White)
            }
        },
        containerColor = BgColor
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Buscar tareas...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = PrimaryBlue
                )
            )

            TabRow(
                selectedTabIndex = when (selectedFilter) {
                    "pendientes" -> 1; "completadas" -> 2; else -> 0
                },
                containerColor = Color.White,
                contentColor = PrimaryBlue
            ) {
                listOf("todas" to "Todas", "pendientes" to "Pendientes", "completadas" to "Completadas")
                    .forEach { (key, label) ->
                        Tab(
                            selected = selectedFilter == key,
                            onClick = { viewModel.setFilter(key) },
                            text = { Text(label) }
                        )
                    }
            }

            val categorias = listOf("Todas", "General", "Trabajo", "Personal", "Estudio", "Salud")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categorias) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            selectedCategory = cat
                            viewModel.setCategory(if (cat == "Todas") null else cat)
                        },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Text(
                "${tasks.size} tarea${if (tasks.size != 1) "s" else ""}",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = Color.Gray, fontSize = 13.sp
            )

            if (tasks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, null,
                            modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(Modifier.height(8.dp))
                        Text("No hay tareas", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggle = { viewModel.toggleTaskCompletion(task) },
                            onEdit = { taskToEdit = task },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        TaskDialog(
            title = "Nueva tarea",
            onDismiss = { showAddDialog = false },
            onConfirm = { desc, priority, category, isRecurring ->
                viewModel.addTask(desc, priority, category, isRecurring)
                NotificationHelper.sendNotification(context, desc)
                showAddDialog = false
            }
        )
    }

    taskToEdit?.let { task ->
        TaskDialog(
            title = "Editar tarea",
            initialDescription = task.description,
            initialPriority = task.priority,
            initialCategory = task.category,
            initialRecurring = task.isRecurring,
            onDismiss = { taskToEdit = null },
            onConfirm = { desc, priority, category, isRecurring ->
                viewModel.editTask(task.copy(
                    description = desc, priority = priority,
                    category = category, isRecurring = isRecurring
                ))
                taskToEdit = null
            }
        )
    }
}

@Composable
fun TaskCard(task: Task, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val priorityColor = when (task.priority) {
        "Alta" -> HighColor; "Media" -> MediumColor; else -> LowColor
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(4.dp).height(52.dp)
                .background(priorityColor, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(12.dp))
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.description,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) Color.Gray else Color.Black
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = RoundedCornerShape(4.dp), color = LightBlue) {
                        Text(task.category, fontSize = 11.sp, color = PrimaryBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Surface(shape = RoundedCornerShape(4.dp), color = priorityColor.copy(alpha = 0.1f)) {
                        Text(task.priority, fontSize = 11.sp, color = priorityColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    if (task.isRecurring) {
                        Icon(Icons.Default.Refresh, null,
                            modifier = Modifier.size(14.dp).align(Alignment.CenterVertically),
                            tint = Color.Gray)
                    }
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, tint = Color.Gray) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = HighColor) }
        }
    }
}

@Composable
fun TaskDialog(
    title: String,
    initialDescription: String = "",
    initialPriority: String = "Media",
    initialCategory: String = "General",
    initialRecurring: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Boolean) -> Unit
) {
    var description by remember { mutableStateOf(initialDescription) }
    var priority by remember { mutableStateOf(initialPriority) }
    var category by remember { mutableStateOf(initialCategory) }
    var isRecurring by remember { mutableStateOf(initialRecurring) }
    var showPriorityMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Box {
                    OutlinedTextField(
                        value = priority, onValueChange = {},
                        label = { Text("Prioridad") }, readOnly = true,
                        trailingIcon = { IconButton(onClick = { showPriorityMenu = true }) {
                            Icon(Icons.Default.ArrowDropDown, null) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(expanded = showPriorityMenu, onDismissRequest = { showPriorityMenu = false }) {
                        listOf("Alta", "Media", "Baja").forEach {
                            DropdownMenuItem(text = { Text(it) },
                                onClick = { priority = it; showPriorityMenu = false })
                        }
                    }
                }
                Box {
                    OutlinedTextField(
                        value = category, onValueChange = {},
                        label = { Text("Categoría") }, readOnly = true,
                        trailingIcon = { IconButton(onClick = { showCategoryMenu = true }) {
                            Icon(Icons.Default.ArrowDropDown, null) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                        listOf("General", "Trabajo", "Personal", "Estudio", "Salud").forEach {
                            DropdownMenuItem(text = { Text(it) },
                                onClick = { category = it; showCategoryMenu = false })
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, null, tint = Color.Gray)
                        Spacer(Modifier.width(8.dp))
                        Text("Tarea recurrente")
                    }
                    Switch(
                        checked = isRecurring, onCheckedChange = { isRecurring = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimaryBlue, checkedTrackColor = LightBlue)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (description.isNotEmpty()) onConfirm(description, priority, category, isRecurring) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}