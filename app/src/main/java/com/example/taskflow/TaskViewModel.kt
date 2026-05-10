package com.example.taskflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaskViewModel(private val dao: TaskDao) : ViewModel() {

    private val _allTasks = MutableStateFlow<List<Task>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow("todas")
    private val _selectedSort = MutableStateFlow("fecha")
    private val _selectedCategory = MutableStateFlow<String?>(null)

    val searchQuery: StateFlow<String> = _searchQuery
    val selectedFilter: StateFlow<String> = _selectedFilter
    val selectedSort: StateFlow<String> = _selectedSort

    val filteredTasks: StateFlow<List<Task>> = combine(
        _allTasks, _searchQuery, _selectedFilter, _selectedSort, _selectedCategory
    ) { tasks, query, filter, sort, category ->
        var result = tasks
        if (query.isNotEmpty()) {
            result = result.filter { it.description.contains(query, ignoreCase = true) }
        }
        result = when (filter) {
            "pendientes" -> result.filter { !it.isCompleted }
            "completadas" -> result.filter { it.isCompleted }
            else -> result
        }
        if (category != null) {
            result = result.filter { it.category == category }
        }
        result = when (sort) {
            "nombre" -> result.sortedBy { it.description.lowercase() }
            "estado" -> result.sortedBy { it.isCompleted }
            "prioridad" -> result.sortedBy {
                when (it.priority) { "Alta" -> 0; "Media" -> 1; else -> 2 }
            }
            else -> result.sortedByDescending { it.createdAt }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { refreshTasks() }
    }

    private suspend fun refreshTasks() {
        _allTasks.value = dao.getAllTasks()
    }

    fun addTask(description: String, priority: String, category: String, isRecurring: Boolean) {
        viewModelScope.launch {
            dao.insertTask(Task(description = description, priority = priority,
                category = category, isRecurring = isRecurring))
            refreshTasks()
        }
    }

    fun editTask(task: Task) {
        viewModelScope.launch { dao.updateTask(task); refreshTasks() }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch { dao.updateTask(task.copy(isCompleted = !task.isCompleted)); refreshTasks() }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch { dao.deleteTask(task); refreshTasks() }
    }

    fun deleteAllTasks() {
        viewModelScope.launch { dao.deleteAllTasks(); _allTasks.value = emptyList() }
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setFilter(filter: String) { _selectedFilter.value = filter }
    fun setSortOrder(sort: String) { _selectedSort.value = sort }
    fun setCategory(category: String?) { _selectedCategory.value = category }
}

class TaskViewModelFactory(private val dao: TaskDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = TaskViewModel(dao) as T
}