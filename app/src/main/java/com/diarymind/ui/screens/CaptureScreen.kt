package com.diarymind.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.diarymind.ui.viewmodel.DiaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    navController: NavController,
    fragmentId: Long? = null,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val existingFragment = fragmentId?.let { id ->
        uiState.fragments.find { it.id == id }
    }
    var content by remember { mutableStateOf("") }
    val isEditMode = fragmentId != null

    LaunchedEffect(existingFragment) {
        existingFragment?.let { content = it.content }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "编辑碎片" else "记录碎片") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("写下你的想法...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                minLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (content.isNotBlank()) {
                        if (isEditMode && fragmentId != null) {
                            viewModel.editFragment(fragmentId, content.trim())
                        } else {
                            viewModel.addFragment(content.trim())
                        }
                        content = ""
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = content.isNotBlank()
            ) {
                Text(if (isEditMode) "保存修改" else "保存碎片")
            }
        }
    }
}