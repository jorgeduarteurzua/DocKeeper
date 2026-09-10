package com.dockeeper.app.ui.categorydetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dockeeper.app.data.repository.CategoryRepository
import com.dockeeper.app.data.repository.ItemRepository
import com.dockeeper.app.domain.model.Category
import com.dockeeper.app.domain.model.Item
import com.dockeeper.app.ui.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryDetailUiState(
    val category: Category? = null,
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val categoryRepository: CategoryRepository,
    private val itemRepository: ItemRepository
) : ViewModel() {

    val categoryId: Long = savedStateHandle.get<Long>(NavArgs.CATEGORY_ID) ?: 0L

    val uiState: StateFlow<CategoryDetailUiState> =
        combine(
            categoryRepository.observeCategory(categoryId),
            itemRepository.observeItems(categoryId)
        ) { category, items ->
            CategoryDetailUiState(category = category, items = items, isLoading = false)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CategoryDetailUiState()
        )

    fun deleteItem(itemId: Long) {
        viewModelScope.launch {
            itemRepository.deleteItem(itemId)
        }
    }
}
