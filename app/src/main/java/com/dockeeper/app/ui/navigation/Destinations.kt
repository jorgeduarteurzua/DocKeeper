package com.dockeeper.app.ui.navigation

/** Nombres de argumentos de navegación. */
object NavArgs {
    const val CATEGORY_ID = "categoryId"
    const val ITEM_ID = "itemId"
    const val ATTACHMENT_ID = "attachmentId"
}

/** Rutas de la app. */
object Routes {
    const val CATEGORIES = "categories"
    const val SEARCH = "search"
    const val ABOUT = "about"

    const val CATEGORY_DETAIL = "category/{${NavArgs.CATEGORY_ID}}"
    fun categoryDetail(categoryId: Long) = "category/$categoryId"

    // Crear/editar item. itemId = -1 significa "nuevo".
    const val ITEM_EDIT = "category/{${NavArgs.CATEGORY_ID}}/item/edit?${NavArgs.ITEM_ID}={${NavArgs.ITEM_ID}}"
    fun itemEdit(categoryId: Long, itemId: Long = -1L) =
        "category/$categoryId/item/edit?${NavArgs.ITEM_ID}=$itemId"

    const val ITEM_DETAIL = "item/{${NavArgs.ITEM_ID}}"
    fun itemDetail(itemId: Long) = "item/$itemId"

    const val ATTACHMENT_VIEWER = "item/{${NavArgs.ITEM_ID}}/attachment/{${NavArgs.ATTACHMENT_ID}}"
    fun attachmentViewer(itemId: Long, attachmentId: Long) =
        "item/$itemId/attachment/$attachmentId"

    const val QR_SCANNER = "qr-scanner"
}
