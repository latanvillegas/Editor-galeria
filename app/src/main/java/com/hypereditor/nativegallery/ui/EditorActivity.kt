package com.hypereditor.nativegallery.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.hypereditor.nativegallery.data.IntentImageLoader
import com.hypereditor.nativegallery.ui.state.EditorIntent
import com.hypereditor.nativegallery.ui.state.EditorViewModel
import com.hypereditor.nativegallery.ui.theme.HyperEditorTheme
import kotlinx.coroutines.launch

class EditorActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val incomingUri: Uri? = IntentImageLoader.extractUriFromIntent(intent)
        if (incomingUri == null) {
            Toast.makeText(this, "No se encontró imagen válida para editar", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        viewModel.handleIntent(EditorIntent.InitializeWithUri(incomingUri))

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    state.exportedUri?.let { finalUri ->
                        val resultIntent = Intent().apply {
                            data = finalUri
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        Toast.makeText(this@EditorActivity, "Imagen guardada con éxito", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }

        setContent {
            HyperEditorTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                HyperEditorScreen(
                    state = uiState,
                    onIntent = { viewModel.handleIntent(it) },
                    onClose = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }
}
