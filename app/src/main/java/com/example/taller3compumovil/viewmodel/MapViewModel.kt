package com.example.taller3compumovil.viewmodel

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MapUiState(
    val isTracking: Boolean = false,
    val currentLocation: LatLng = LatLng(4.627389, -74.064163), // Bogotá por defecto
    val errorMessage: String? = null
)

class MapViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun toggleTracking(isTracking: Boolean) {
        _uiState.update { it.copy(isTracking = isTracking) }

        if (isTracking) {
            // Aquí irá la lógica del GPS después
        } else {
            // Aquí irá la lógica para apagar el GPS después
        }
    }
}