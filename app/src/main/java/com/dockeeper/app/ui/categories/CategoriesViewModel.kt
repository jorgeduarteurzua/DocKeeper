package com.dockeeper.app.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dockeeper.app.data.repository.CategoryRepository
import com.dockeeper.app.domain.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val uiState: StateFlow<CategoriesUiState> =
        categoryRepository.observeCategories()
            .map { CategoriesUiState(categories = it, isLoading = false) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = CategoriesUiState()
            )

    fun createCategory(name: String, colorTag: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            categoryRepository.createCategory(name, colorTag)
        }
    }

    fun updateCategory(id: Long, name: String, colorTag: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            categoryRepository.renameCategory(id, name, colorTag)
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(id)
        }
    }
}
