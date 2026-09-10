package com.dockeeper.app.ui.itemedit

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dockeeper.app.data.file.CameraTarget
import com.dockeeper.app.data.file.FileManager
import com.dockeeper.app.data.local.entity.AttachmentType
import com.dockeeper.app.data.repository.ItemRepository
import com.dockeeper.app.domain.model.Attachment
import com.dockeeper.app.ui.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ItemEditUiState(
    val itemId: Long = -1L,
    val title: String = "",
    val description: String = "",
    val attachments: List<Attachment> = emptyList(),
    val isNew: Boolean = true,
    val isReady: Boolean = false,
    val isSaving: Boolean = false
)

@HiltViewModel
class ItemEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val itemRepository: ItemRepository,
    private val fileManager: FileManager
) : ViewModel() {

    private val categoryId: Long = savedStateHandle.get<Long>(NavArgs.CATEGORY_ID) ?: 0L
    private val incomingItemId: Long = savedStateHandle.get<Long>(NavArgs.ITEM_ID) ?: -1L

    /** Crea el archivo destino para que la cámara escriba la foto. */
    fun createCameraTarget(): CameraTarget = fileManager.createImageTarget()

    private val _uiState = MutableStateFlow(ItemEditUiState())
    val uiState: StateFlow<ItemEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (incomingItemId > 0) {
                // Edición de item existente.
                loadExisting(incomingItemId)
            } else {
                // Nuevo: crear borrador vacío para poder adjuntar de inmediato.
                val newId = itemRepository.createItem(categoryId, title = "", description = "")
                _uiState.update {
                    it.copy(itemId = newId, isNew = true, isReady = true)
                }
                observeAttachments(newId)
            }
        }
    }

    private fun loadExisting(itemId: Long) {
        viewModelScope.launch {
            itemRepository.observeItemDetail(itemId).collect { detail ->
                if (detail != null) {
                    _uiState.update {
                        it.copy(
                            itemId = detail.item.id,
                            title = if (it.isReady) it.title else detail.item.title,
                            description = if (it.isReady) it.description else detail.item.description,
                            attachments = detail.attachments,
                            isNew = false,
                            isReady = true
                        )
                    }
                }
            }
        }
    }

    private fun observeAttachments(itemId: Long) {
        viewModelScope.launch {
            itemRepository.observeItemDetail(itemId).collect { detail ->
                if (detail != null) {
                    _uiState.update { it.copy(attachments = detail.attachments) }
                }
            }
        }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }

    fun addFileAttachment(uri: Uri, type: AttachmentType) {
        val id = _uiState.value.itemId
        if (id <= 0) return
        viewModelScope.launch {
            itemRepository.addFileAttachment(id, uri, type)
        }
    }

    fun addCameraPhoto(filePath: String) {
        val id = _uiState.value.itemId
        if (id <= 0) return
        viewModelScope.launch {
            itemRepository.addCameraPhoto(id, filePath)
        }
    }

    fun addLink(content: String, displayName: String, type: AttachmentType) {
        val id = _uiState.value.itemId
        if (id <= 0 || content.isBlank()) return
        viewModelScope.launch {
            itemRepository.addLinkAttachment(id, content, displayName, type)
        }
    }

    fun deleteAttachment(attachmentId: Long) {
        viewModelScope.launch {
            itemRepository.deleteAttachment(attachmentId)
        }
    }

    /**
     * Guarda título/descripción. Devuelve true si guardó (título no vacío).
     */
    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        val id = state.itemId
        if (id <= 0) return
        val title = state.title.trim().ifBlank { "Sin título" }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            itemRepository.updateItem(id, title, state.description)
            _uiState.update { it.copy(isSaving = false) }
            onSaved()
        }
    }

    /**
     * Si el usuario descarta un item nuevo sin contenido, lo eliminamos para no
     * dejar borradores vacíos.
     */
    fun discardIfEmpty() {
        val state = _uiState.value
        if (state.isNew && state.title.isBlank() && state.attachments.isEmpty()) {
            val id = state.itemId
            if (id > 0) {
                viewModelScope.launch { itemRepository.deleteItem(id) }
            }
        } else if (state.isNew && state.title.isBlank() && state.attachments.isNotEmpty()) {
            // Tiene adjuntos pero no título: guardamos con título por defecto.
            val id = state.itemId
            if (id > 0) {
                viewModelScope.launch { itemRepository.updateItem(id, "Sin título", state.description) }
            }
        }
    }
}
