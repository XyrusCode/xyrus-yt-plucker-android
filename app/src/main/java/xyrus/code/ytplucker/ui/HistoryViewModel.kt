package xyrus.code.ytplucker.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyrus.code.ytplucker.data.HistoryStore
import xyrus.code.ytplucker.domain.model.DownloadedFile

class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val _items = MutableStateFlow<List<DownloadedFile>>(emptyList())
    val items: StateFlow<List<DownloadedFile>> = _items.asStateFlow()

    /** Re-query MediaStore. Cheap; call when the History tab becomes visible. */
    fun refresh() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { HistoryStore.list(getApplication()) }
            _items.value = list
        }
    }

    fun dismiss(file: DownloadedFile) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { HistoryStore.dismiss(getApplication(), file.uri) }
            _items.value = _items.value - file
        }
    }

    fun dismissAll() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { HistoryStore.dismissAll(getApplication()) }
            _items.value = emptyList()
        }
    }
}
