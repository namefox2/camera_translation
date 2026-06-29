package com.letsgo.translator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun HowToUseScreen(onStart: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "사용 방법",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "번역 앱을 시작하기 전에 이것만 알아두세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(8.dp))
        }

        val features = listOf(
            Triple(Icons.Default.CameraAlt, "AR 번역",
                "카메라를 텍스트에 갖다 대면 화면 위에 번역이 바로 표시됩니다. 자물쇠 버튼으로 화면을 고정하고 번역 결과를 천천히 볼 수 있습니다."),
            Triple(Icons.Default.PhotoCamera, "카메라 번역",
                "사진을 찍거나 갤러리에서 이미지를 선택해 원하는 영역을 드래그하면 해당 부분을 번역합니다."),
            Triple(Icons.Default.Mic, "음성 번역",
                "말하면 자동으로 인식해 번역합니다.")
        )

        items(features) { (icon, title, desc) ->
            FeatureCard(icon = icon, title = title, description = desc)
        }

        item {
            Spacer(Modifier.height(4.dp))
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))
        }

        val tips = listOf(
            Triple(Icons.Default.WifiOff, "오프라인 사용 가능",
                "언어를 다운받으시면 오프라인에서 사용 가능하나, 단어 단위로 해석될 수 있습니다."),
            Triple(Icons.Default.Wifi, "다운로드 안내",
                "언어 다운로드 시 와이파이를 권장합니다. 모바일 데이터 사용 시 아주 오래 걸릴 수 있습니다."),
            Triple(Icons.Default.Palette, "테마 선택",
                "설정에서 다양한 테마를 선택하실 수 있습니다."),
            Triple(Icons.Default.Favorite, "개발자 후원",
                "개발자 후원도 가능합니다 ㅎㅎ  설정 → 개발자 후원하기")
        )

        items(tips) { (icon, title, text) ->
            TipRow(icon = icon, title = title, text = text)
        }

        item {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("시작하기", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FeatureCard(icon: ImageVector, title: String, description: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
private fun TipRow(icon: ImageVector, title: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(2.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}
