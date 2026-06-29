package com.letsgo.translator.legal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class OssLibrary(
    val name: String,
    val version: String,
    val author: String,
    val licenseType: String,
    val licenseText: String,
    val url: String
)

private val APACHE_2 = """
Apache License
Version 2.0, January 2004
http://www.apache.org/licenses/

TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

Copyright [yyyy] [name of copyright owner]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
""".trimIndent()

private val libraries = listOf(
    OssLibrary("Jetpack Compose", "2024.06.00", "Google LLC",
        "Apache License 2.0", APACHE_2, "https://developer.android.com/jetpack/compose"),
    OssLibrary("CameraX", "1.3.4", "Google LLC",
        "Apache License 2.0", APACHE_2, "https://developer.android.com/training/camerax"),
    OssLibrary("ML Kit Text Recognition", "16.0.1", "Google LLC",
        "Apache License 2.0", APACHE_2, "https://developers.google.com/ml-kit/vision/text-recognition"),
    OssLibrary("ML Kit Translate", "17.0.3", "Google LLC",
        "Apache License 2.0", APACHE_2, "https://developers.google.com/ml-kit/language/translation"),
    OssLibrary("ML Kit Language ID", "17.0.6", "Google LLC",
        "Apache License 2.0", APACHE_2, "https://developers.google.com/ml-kit/language/identification"),
    OssLibrary("Hilt (Dagger)", "2.51.1", "Google LLC",
        "Apache License 2.0", APACHE_2, "https://dagger.dev/hilt/"),
    OssLibrary("Hilt Navigation Compose", "1.2.0", "Google LLC",
        "Apache License 2.0", APACHE_2, "https://developer.android.com/jetpack/compose/libraries#hilt"),
    OssLibrary("Navigation Compose", "2.7.7", "Google LLC",
        "Apache License 2.0", APACHE_2, "https://developer.android.com/jetpack/compose/navigation"),
    OssLibrary("Room", "2.6.1", "Google LLC",
        "Apache License 2.0", APACHE_2, "https://developer.android.com/training/data-storage/room"),
    OssLibrary("DataStore Preferences", "1.1.1", "Google LLC",
        "Apache License 2.0", APACHE_2, "https://developer.android.com/topic/libraries/architecture/datastore"),
    OssLibrary("Retrofit", "2.11.0", "Square, Inc.",
        "Apache License 2.0", APACHE_2, "https://square.github.io/retrofit/"),
    OssLibrary("OkHttp", "4.12.0", "Square, Inc.",
        "Apache License 2.0", APACHE_2, "https://square.github.io/okhttp/"),
    OssLibrary("Accompanist Permissions", "0.34.0", "Google LLC",
        "Apache License 2.0", APACHE_2, "https://google.github.io/accompanist/permissions/"),
    OssLibrary("Kotlin Coroutines", "1.8.1", "JetBrains s.r.o.",
        "Apache License 2.0", APACHE_2, "https://github.com/Kotlin/kotlinx.coroutines"),
    OssLibrary("Gson", "2.10.1", "Google LLC",
        "Apache License 2.0", APACHE_2, "https://github.com/google/gson"),
    OssLibrary("Coil", "2.7.0", "Coil Contributors",
        "Apache License 2.0", APACHE_2, "https://github.com/coil-kt/coil"),
    OssLibrary("Lottie for Android", "6.4.1", "Airbnb, Inc.",
        "Apache License 2.0", APACHE_2, "https://github.com/airbnb/lottie-android"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSourceLicensesScreen(onBack: () -> Unit) {
    var selectedLibrary by remember { mutableStateOf<OssLibrary?>(null) }

    if (selectedLibrary != null) {
        LicenseDetailScreen(
            library = selectedLibrary!!,
            onBack = { selectedLibrary = null }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("오픈소스 라이선스") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Text(
                    "본 앱은 아래 오픈소스 라이브러리를 사용합니다.\n" +
                        "모두 Apache License 2.0 하에 배포됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                HorizontalDivider()
            }

            items(libraries) { lib ->
                ListItem(
                    headlineContent = { Text(lib.name, fontWeight = FontWeight.Medium) },
                    supportingContent = {
                        Text("v${lib.version} · ${lib.author}  |  ${lib.licenseType}",
                            style = MaterialTheme.typography.bodySmall)
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    },
                    modifier = Modifier.clickable { selectedLibrary = lib }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LicenseDetailScreen(library: OssLibrary, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(library.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
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
            Text("버전: ${library.version}", style = MaterialTheme.typography.bodySmall)
            Text("제작: ${library.author}", style = MaterialTheme.typography.bodySmall)
            Text("URL: ${library.url}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text(
                text = library.licenseText,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
