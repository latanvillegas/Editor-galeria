package com.hypereditor.nativegallery.domain.history

import com.hypereditor.nativegallery.domain.model.EditorDocument

class HistoryManager(private val maxStackSize: Int = 50) {
    private val undoStack = ArrayDeque<EditorDocument>()
    private val redoStack = ArrayDeque<EditorDocument>()

    fun pushState(document: EditorDocument) {
        // Prevent duplicate consecutive states
        if (undoStack.isNotEmpty() && undoStack.last() == document) {
            return
        }

        if (undoStack.size >= maxStackSize) {
            undoStack.removeFirst()
        }
        undoStack.addLast(document)
        redoStack.clear()
    }

    fun undo(currentDocument: EditorDocument): EditorDocument? {
        if (undoStack.isEmpty()) return null
        val previousState = undoStack.removeLast()
        redoStack.addLast(currentDocument)
        return previousState
    }

    fun redo(currentDocument: EditorDocument): EditorDocument? {
        if (redoStack.isEmpty()) return null
        val nextState = redoStack.removeLast()
        undoStack.addLast(currentDocument)
        return nextState
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoCount: Int get() = undoStack.size
    val redoCount: Int get() = redoStack.size
}
