package com.hypereditor.nativegallery.domain.history

import com.hypereditor.nativegallery.domain.model.EditorDocument

class HistoryManager(private val maxStackSize: Int = 30) {
    private val undoStack = ArrayDeque<EditorDocument>()
    private val redoStack = ArrayDeque<EditorDocument>()

    fun pushState(document: EditorDocument) {
        if (undoStack.size >= maxStackSize) {
            undoStack.removeFirst()
        }
        undoStack.addLast(document)
        redoStack.clear()
    }

    fun undo(currentDocument: EditorDocument): EditorDocument? {
        if (undoStack.isEmpty()) return null
        redoStack.addLast(currentDocument)
        return undoStack.removeLast()
    }

    fun redo(currentDocument: EditorDocument): EditorDocument? {
        if (redoStack.isEmpty()) return null
        undoStack.addLast(currentDocument)
        return redoStack.removeLast()
    }

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
}
