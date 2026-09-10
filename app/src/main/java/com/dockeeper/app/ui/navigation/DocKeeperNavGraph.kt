package com.dockeeper.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dockeeper.app.ui.about.AboutScreen
import com.dockeeper.app.ui.categories.CategoriesScreen
import com.dockeeper.app.ui.categorydetail.CategoryDetailScreen
import com.dockeeper.app.ui.itemdetail.ItemDetailScreen
import com.dockeeper.app.ui.itemedit.ItemEditScreen
import com.dockeeper.app.ui.qr.QrScannerScreen
import com.dockeeper.app.ui.search.SearchScreen
import com.dockeeper.app.ui.viewer.AttachmentViewerScreen

const val QR_RESULT_KEY = "qr_result"

@Composable
fun DocKeeperNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.CATEGORIES) {

        composable(Routes.CATEGORIES) {
            CategoriesScreen(
                onCategoryClick = { id -> navController.navigate(Routes.categoryDetail(id)) },
                onSearchClick = { navController.navigate(Routes.SEARCH) },
                onAboutClick = { navController.navigate(Routes.ABOUT) }
            )
        }

        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onResultClick = { itemId -> navController.navigate(Routes.itemDetail(itemId)) }
            )
        }

        composable(
            route = Routes.CATEGORY_DETAIL,
            arguments = listOf(navArgument(NavArgs.CATEGORY_ID) { type = NavType.LongType })
        ) {
            CategoryDetailScreen(
                onBack = { navController.popBackStack() },
                onAddItem = { categoryId ->
                    navController.navigate(Routes.itemEdit(categoryId, -1L))
                },
                onItemClick = { itemId -> navController.navigate(Routes.itemDetail(itemId)) },
                onEditItem = { categoryId, itemId ->
                    navController.navigate(Routes.itemEdit(categoryId, itemId))
                }
            )
        }

        composable(
            route = Routes.ITEM_EDIT,
            arguments = listOf(
                navArgument(NavArgs.CATEGORY_ID) { type = NavType.LongType },
                navArgument(NavArgs.ITEM_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            // Recuperar el resultado del QR guardado en este backstack entry.
            val qrResult by backStackEntry.savedStateHandle
                .getStateFlow<String?>(QR_RESULT_KEY, null)
                .collectAsStateWithLifecycle()

            ItemEditScreen(
                onDone = { navController.popBackStack() },
                onScanQr = { navController.navigate(Routes.QR_SCANNER) },
                qrResult = qrResult,
                onQrConsumed = {
                    backStackEntry.savedStateHandle.remove<String>(QR_RESULT_KEY)
                }
            )
        }

        composable(
            route = Routes.ITEM_DETAIL,
            arguments = listOf(navArgument(NavArgs.ITEM_ID) { type = NavType.LongType })
        ) {
            ItemDetailScreen(
                onBack = { navController.popBackStack() },
                onAddMore = { categoryId, itemId ->
                    navController.navigate(Routes.itemEdit(categoryId, itemId))
                },
                onAttachmentClick = { itemId, attachmentId ->
                    navController.navigate(Routes.attachmentViewer(itemId, attachmentId))
                }
            )
        }

        composable(
            route = Routes.ATTACHMENT_VIEWER,
            arguments = listOf(
                navArgument(NavArgs.ITEM_ID) { type = NavType.LongType },
                navArgument(NavArgs.ATTACHMENT_ID) { type = NavType.LongType }
            )
        ) {
            AttachmentViewerScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.QR_SCANNER) {
            QrScannerScreen(
                onBack = { navController.popBackStack() },
                onResult = { value ->
                    // Devolver el resultado a la pantalla de edición (entry anterior).
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(QR_RESULT_KEY, value)
                    navController.popBackStack()
                }
            )
        }
    }
}
