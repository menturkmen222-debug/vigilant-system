package com.whiteboard.animator.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whiteboard.animator.data.model.ApiProvider
import com.whiteboard.animator.data.model.VideoStyle
import com.whiteboard.animator.ui.components.StyleChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val defaultVideoStyle by viewModel.defaultVideoStyle.collectAsStateWithLifecycle()
    val apiKeysMap by viewModel.apiKeysMap.collectAsStateWithLifecycle()
    
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAddKeyDialog by remember { mutableStateOf(false) }
    var selectedProviderForKey by remember { mutableStateOf<ApiProvider?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ================== GENERAL SETTINGS ==================
            SettingsSectionTitle("General")
            
            // Dark Mode
            ListItem(
                headlineContent = { Text("Dark Mode") },
                supportingContent = { Text("Toggle dark theme") },
                trailingContent = {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.setDarkMode(it) }
                    )
                }
            )
            
            // Language
            ListItem(
                headlineContent = { Text("Language") },
                supportingContent = { Text(getLanguageDisplayName(language)) },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                },
                modifier = Modifier.clickable { showLanguageDialog = true }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // ================== VIDEO STYLE ==================
            SettingsSectionTitle("Default Video Style")
            Text(
                "Choose the default visual style for new projects",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(VideoStyle.entries) { style ->
                    StyleChip(
                        style = style,
                        isSelected = style == defaultVideoStyle,
                        onClick = { viewModel.setDefaultVideoStyle(style) }
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            // ================== API KEYS ==================
            SettingsSectionTitle("API Keys")
            Text(
                "Configure API keys for AI-powered features. Multiple keys per provider enable rotation and fallback.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Legacy single key (backward compatibility)
            OutlinedTextField(
                value = apiKey,
                onValueChange = { viewModel.updateApiKey(it) },
                label = { Text("Quick API Key (Legacy)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("For simple single-key setups") }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Multi-provider key management
            ApiProvider.entries.forEach { provider ->
                ApiProviderSection(
                    provider = provider,
                    keys = apiKeysMap[provider] ?: emptyList(),
                    onAddKey = {
                        selectedProviderForKey = provider
                        showAddKeyDialog = true
                    },
                    onRemoveKey = { key -> viewModel.removeApiKey(provider, key) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            // ================== ABOUT ==================
            SettingsSectionTitle("About")
            ListItem(
                headlineContent = { Text("Whiteboard Animator Pro") },
                supportingContent = { Text("Version 2.0.0") }
            )
            ListItem(
                headlineContent = { Text("License") },
                supportingContent = { Text("Open Source / Free") }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ListItem(
                headlineContent = { Text(stringResource(com.whiteboard.animator.R.string.developer_label)) },
                supportingContent = { Text(stringResource(com.whiteboard.animator.R.string.developer_name)) }
            )
            ListItem(
                headlineContent = { Text(stringResource(com.whiteboard.animator.R.string.developer_region_label)) },
                supportingContent = { Text(stringResource(com.whiteboard.animator.R.string.developer_region)) }
            )
        }
    }
    
    // Language picker dialog
    if (showLanguageDialog) {
        LanguagePickerDialog(
            currentLanguage = language,
            onLanguageSelected = { 
                viewModel.setLanguage(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
    
    // Add API key dialog
    if (showAddKeyDialog && selectedProviderForKey != null) {
        AddApiKeyDialog(
            provider = selectedProviderForKey!!,
            onConfirm = { key ->
                viewModel.addApiKey(selectedProviderForKey!!, key)
                showAddKeyDialog = false
                selectedProviderForKey = null
            },
            onDismiss = {
                showAddKeyDialog = false
                selectedProviderForKey = null
            }
        )
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun ApiProviderSection(
    provider: ApiProvider,
    keys: List<String>,
    onAddKey: () -> Unit,
    onRemoveKey: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = getProviderDisplayName(provider),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "${keys.size} key(s) configured",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onAddKey) {
                    Icon(Icons.Default.Add, contentDescription = "Add key")
                }
            }
            
            if (keys.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                keys.forEachIndexed { index, key ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Key ${index + 1}: ${maskApiKey(key)}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onRemoveKey(key) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguagePickerDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = listOf(
        "en" to "English",
        "uz" to "O'zbek (Uzbek)",
        "tk" to "Türkmen (Turkmen)"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Language") },
        text = {
            Column {
                languages.forEach { (code, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(code) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = code == currentLanguage,
                            onClick = { onLanguageSelected(code) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddApiKeyDialog(
    provider: ApiProvider,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var key by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${getProviderDisplayName(provider)} Key") },
        text = {
            Column {
                Text(
                    text = getProviderInstructions(provider),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("API Key") },
                    placeholder = { Text("Paste your API key...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(key) },
                enabled = key.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getLanguageDisplayName(code: String): String {
    return when (code) {
        "en" -> "English"
        "uz" -> "O'zbek (Uzbek)"
        "tk" -> "Türkmen (Turkmen)"
        else -> "English"
    }
}

private fun getProviderDisplayName(provider: ApiProvider): String {
    return when (provider) {
        ApiProvider.GEMINI -> "Google Gemini"
        ApiProvider.OPENAI -> "OpenAI"
        ApiProvider.HUGGINGFACE -> "Hugging Face"
        ApiProvider.ELEVENLABS -> "ElevenLabs"
        ApiProvider.AZURE_TTS -> "Azure TTS"
    }
}

private fun getProviderInstructions(provider: ApiProvider): String {
    return when (provider) {
        ApiProvider.GEMINI -> "Get your free API key at: aistudio.google.com"
        ApiProvider.OPENAI -> "Get your API key at: platform.openai.com"
        ApiProvider.HUGGINGFACE -> "Get your free API key at: huggingface.co/settings/tokens"
        ApiProvider.ELEVENLABS -> "Get your API key at: elevenlabs.io"
        ApiProvider.AZURE_TTS -> "Get your API key from Azure Portal"
    }
}

private fun maskApiKey(key: String): String {
    return if (key.length > 8) {
        "${key.take(4)}...${key.takeLast(4)}"
    } else {
        "****"
    }
}
