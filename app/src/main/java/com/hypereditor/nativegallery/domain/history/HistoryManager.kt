package com.hypereditor.nativegallery.domain.history

import com.hypereditor.nativegallery.domain.model.EditorDocument
import java.util.UUID

data class HistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val actionName: String,
    val document: EditorDocument,
    val timestamp: Long = System.currentTimeMillis()
)

class HistoryManager(private val maxStackSize: Int = 50) {
    private val undoStack = ArrayDeque<HistoryItem>()
    private val redoStack = ArrayDeque<HistoryItem>()

    fun pushState(document: EditorDocument, actionName: String = "Operación") {
        // Prevent duplicate consecutive states
        if (undoStack.isNotEmpty() && undoStack.last().document == document) {
            return
        }

        if (undoStack.size >= maxStackSize) {
            undoStack.removeFirst()
        }
        undoStack.addLast(HistoryItem(actionName = actionName, document = document))
        // Limpiar redo cuando se hace una nueva operación después de undo
        redoStack.clear()
    }

    fun undo(currentDocument: EditorDocument, currentActionName: String = "Edición actual"): Pair<EditorDocument, String>? {
        if (undoStack.isEmpty()) return null
        val previousEntry = undoStack.removeLast()
        redoStack.addLast(HistoryItem(actionName = currentActionName, document = currentDocument))
        return Pair(previousEntry.document, previousEntry.actionName)
    }

    fun redo(currentDocument: EditorDocument, currentActionName: String = "Edición previa"): Pair<EditorDocument, String>? {
        if (redoStack.isEmpty()) return null
        val nextEntry = redoStack.removeLast()
        undoStack.addLast(HistoryItem(actionName = currentActionName, document = currentDocument))
        return Pair(nextEntry.document, nextEntry.actionName)
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoCount: Int get() = undoStack.size
    val redoCount: Int get() = redoStack.size
    val nextUndoActionName: String? get() = undoStack.lastOrNull()?.actionName
    val nextRedoActionName: String? get() = redoStack.lastOrNull()?.actionName

    fun getOperationsHistory(): List<String> {
        val list = mutableListOf<String>()
        for (item in undoStack) {
            list.add(item.actionName)
        }
        return list
    }
}
