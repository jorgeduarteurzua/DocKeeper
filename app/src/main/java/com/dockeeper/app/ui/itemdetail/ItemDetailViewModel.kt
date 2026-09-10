package com.dockeeper.app.ui.itemdetail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dockeeper.app.data.repository.ItemRepository
import com.dockeeper.app.domain.model.ItemDetail
import com.dockeeper.app.ui.navigation.NavArgs
import com.dockeeper.app.util.PdfComposer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ItemDetailUiState(
    val detail: ItemDetail? = null,
    val isLoading: Boolean = true
)

/** Estado de la generación del PDF combinado del item. */
sealed interface PdfState {
    data object Idle : PdfState
    data object Generating : PdfState
    data class Success(val file: File) : PdfState
    data class Error(val message: String) : PdfState
}

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val itemRepository: ItemRepository
) : ViewModel() {

    val itemId: Long = savedStateHandle.get<Long>(NavArgs.ITEM_ID) ?: 0L

    val uiState: StateFlow<ItemDetailUiState> =
        itemRepository.observeItemDetail(itemId)
            .map { ItemDetailUiState(detail = it, isLoading = false) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ItemDetailUiState()
            )

    private val _pdfState = MutableStateFlow<PdfState>(PdfState.Idle)
    val pdfState: StateFlow<PdfState> = _pdfState.asStateFlow()

    fun deleteAttachment(attachmentId: Long) {
        viewModelScope.launch {
            itemRepository.deleteAttachment(attachmentId)
        }
    }

    fun deleteItem(onDeleted: () -> Unit) {
        viewModelScope.launch {
            itemRepository.deleteItem(itemId)
            onDeleted()
        }
    }

    /** Genera un único PDF con todo el contenido del item. */
    fun generatePdf() {
        val detail = uiState.value.detail ?: return
        if (_pdfState.value is PdfState.Generating) return
        _pdfState.value = PdfState.Generating
        viewModelScope.launch {
            try {
                val outputDir = File(context.cacheDir, "exports")
                val file = PdfComposer.buildItemPdf(context, detail, outputDir)
                _pdfState.value = PdfState.Success(file)
            } catch (e: Exception) {
                _pdfState.value = PdfState.Error(e.message ?: "No se pudo generar el PDF")
            }
        }
    }

    fun consumePdfState() {
        _pdfState.value = PdfState.Idle
    }
}
