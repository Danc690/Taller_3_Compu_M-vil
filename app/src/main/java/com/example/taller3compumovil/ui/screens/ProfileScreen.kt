package com.example.taller3compumovil.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.taller3compumovil.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onUnauthenticated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }
    LaunchedEffect(uiState.isUnauthenticated) {
        if (uiState.isUnauthenticated) onUnauthenticated()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Mi perfil", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.isLoading && uiState.profile == null) {
            CircularProgressIndicator()
        } else if (uiState.profile != null) {
            val profile = uiState.profile

            if (profile?.photoUrl != null) {
                Text("Fotografía de perfil disponible")
                Text(
                    text = profile.photoUrl,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre") },
                singleLine = true,
                enabled = uiState.isEditing && !uiState.isSaving
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = profile?.email.orEmpty(),
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Correo electrónico") },
                singleLine = true,
                readOnly = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.phone,
                onValueChange = viewModel::onPhoneChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Teléfono") },
                singleLine = true,
                enabled = uiState.isEditing && !uiState.isSaving
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (uiState.isEditing) {
                Button(
                    onClick = viewModel::saveChanges,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving
                ) {
                    Text("Guardar cambios")
                }
                TextButton(
                    onClick = viewModel::cancelEditing,
                    enabled = !uiState.isSaving
                ) {
                    Text("Cancelar")
                }
            } else {
                Button(
                    onClick = viewModel::startEditing,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving
                ) {
                    Text("Editar perfil")
                }
            }
        } else if (uiState.errorMessage != null && !uiState.isUnauthenticated) {
            Button(
                onClick = viewModel::loadProfile,
                enabled = !uiState.isLoading
            ) {
                Text("Reintentar consulta")
            }
        }

        if (uiState.isSaving || (uiState.isLoading && uiState.profile != null)) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }

        uiState.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        uiState.successMessage?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
