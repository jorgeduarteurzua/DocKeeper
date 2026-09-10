package com.dockeeper.app.ui.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dockeeper.app.data.repository.ItemRepository
import com.dockeeper.app.domain.model.Attachment
import com.dockeeper.app.ui.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttachmentViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val itemId: Long = savedStateHandle.get<Long>(NavArgs.ITEM_ID) ?: 0L
    private val attachmentId: Long = savedStateHandle.get<Long>(NavArgs.ATTACHMENT_ID) ?: 0L

    val attachment: StateFlow<Attachment?> =
        itemRepository.observeItemDetail(itemId)
            .map { detail -> detail?.attachments?.firstOrNull { it.id == attachmentId } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            itemRepository.deleteAttachment(attachmentId)
            onDeleted()
        }
    }
}
