package com.example.camera

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {

    // esto es solo para que se muestren las fotos de la sesion y no las que ya se tomaron
    private val _photos = MutableStateFlow<List<String>>(emptyList())
    val photos: StateFlow<List<String>> = _photos.asStateFlow()

    // se usa para cuando una foto se haya guardado bien en almacenamiento externo
    fun onPhotoSaved(uri: String) {
        if (uri.isNotBlank()) {
            _photos.value = _photos.value + uri
        }
    }

    // Elimina una foto de la galería de la sesión pero no borra el archivo del disco
    fun removePhoto(uri: String) {
        _photos.value = _photos.value.filterNot { it == uri }
    }

    // Limpia la galería de la sesión NO del disco
    fun clearSession() {
        _photos.value = emptyList()
    }
}
