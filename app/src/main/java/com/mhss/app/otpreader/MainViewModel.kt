package com.mhss.app.otpreader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhss.app.otpreader.data.datastore.DataStoreRepository
import com.mhss.app.otpreader.data.repository.MainRepository
import com.mhss.app.otpreader.model.InstalledApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MainRepository
): ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val installedApps by lazy {
        repository.getInstalledApps()
    }

    private val selectedApps = repository.getPref(DataStoreRepository.PACKAGES, emptySet())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptySet())

    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val apps: StateFlow<List<InstalledApp>> = _apps

    private val _mustContainText = MutableStateFlow("")
    val mustContainText: StateFlow<String> = _mustContainText

    init {
        viewModelScope.launch {
            _mustContainText.update {
                repository.getPref(DataStoreRepository.CONTAINS, emptySet())
                    .map { it.joinToString(", ") }.first()
            }
            combine(
                searchQuery,
                selectedApps
            ) { q, selected ->
                _apps.update {
                    installedApps.map {
                        it.copy(included = selected.contains(it.packageName))
                    }.filter {
                        it.name.contains(q.trim(), ignoreCase = true)
                    }.sortedBy { it.name }
                }
            }.collect()
            mustContainText.debounce(1000).onEach {text ->
                repository.savePref(
                    DataStoreRepository.CONTAINS,
                    text.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
                )
            }.collect()
        }
    }

    fun searchApp(query: String) {
        _searchQuery.update { query }
    }

    fun onAppSelected(app: InstalledApp, selected: Boolean) = viewModelScope.launch {
        val apps = if (selected) {
            selectedApps.value + app.packageName
        } else {
            selectedApps.value - app.packageName
        }
        repository.savePref(DataStoreRepository.PACKAGES, apps)
    }

    fun saveContains(text: String) = viewModelScope.launch {
        _mustContainText.update { text }
        val contains = text.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
        repository.savePref(DataStoreRepository.CONTAINS, contains)
    }

}