package com.example.artranslator.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 방송통신위원회 「스마트폰 앱 접근권한 개인정보보호 안내서」 기준
 * 앱 최초 실행 시 접근권한 목록과 수집·이용 목적을 고지하는 필수 팝업.
 *
 * 필수 권한 / 선택 권한을 구분하여 표시하며,
 * 사용자가 [확인 및 동의] 버튼을 누르면 [onAcknowledge]가 호출됩니다.
 */
@Composable
fun PermissionNoticeDialog(
    onAcknowledge: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* 뒤로가기로 닫기 불가 – 법적 고지 의무 */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // ─ 헤더 ───────────────────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "앱 접근권한 안내",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "AR Translator 앱은 서비스 제공을 위해 아래 접근권한을 사용합니다.\n" +
                        "접근권한은 해당 기능 사용 시에만 활성화됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                // ─ 스크롤 영역 ─────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 필수 권한
                    PermissionSectionHeader(
                        title = "필수 접근권한",
                        subtitle = "서비스의 핵심 기능 구현에 반드시 필요합니다.\n" +
                            "허용하지 않으면 해당 기능을 이용하실 수 없습니다."
                    )
                    Spacer(Modifier.height(8.dp))

                    PermissionItem(
                        icon = Icons.Default.CameraAlt,
                        name = "카메라",
                        purpose = "AR 카메라 번역 — 카메라 영상에서 텍스트를 인식하여 실시간 번역 오버레이를 표시합니다.",
                        isRequired = true
                    )
                    PermissionItem(
                        icon = Icons.Default.Mic,
                        name = "마이크 (음성 녹음)",
                        purpose = "음성 통역 — 사용자의 발화를 인식하여 번역합니다. " +
                            "녹음은 사용자가 마이크 버튼을 누른 동안에만 활성화됩니다.",
                        isRequired = true
                    )
                    PermissionItem(
                        icon = Icons.Default.Wifi,
                        name = "인터넷 연결",
                        purpose = "온라인 번역 API 연결 — Google Cloud Translation API를 통해 고품질 번역을 제공합니다.",
                        isRequired = true
                    )

                    Spacer(Modifier.height(16.dp))

                    // 선택 권한
                    PermissionSectionHeader(
                        title = "선택 접근권한",
                        subtitle = "허용하지 않아도 해당 기능 외의 서비스는 이용 가능합니다."
                    )
                    Spacer(Modifier.height(8.dp))

                    PermissionItem(
                        icon = Icons.Default.NetworkCheck,
                        name = "네트워크 상태 확인",
                        purpose = "온라인/오프라인 상태를 감지하여 적합한 번역 방식(Cloud API 또는 오프라인 ML Kit)을 자동 선택합니다.",
                        isRequired = false
                    )

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    // 개인정보 처리 고지
                    DataProcessingNotice()

                    Spacer(Modifier.height(12.dp))

                    // 음성 녹음 주의사항
                    VoiceRecordingWarning()
                }

                // ─ 동의 버튼 ──────────────────────────────────────────────────
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onAcknowledge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "확인 및 동의",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "위 내용을 확인하고 접근권한 사용에 동의합니다.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ─── 하위 컴포넌트 ─────────────────────────────────────────────────────────────

@Composable
private fun PermissionSectionHeader(title: String, subtitle: String) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    name: String,
    purpose: String,
    isRequired: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = if (isRequired)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isRequired)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp))
                Surface(
                    color = if (isRequired)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        if (isRequired) "필수" else "선택",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isRequired)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                purpose,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun DataProcessingNotice() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    "개인정보 처리 안내",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "• 카메라로 인식된 텍스트와 입력 텍스트는 번역을 위해 Google Cloud Translation API로 전송될 수 있습니다.\n" +
                        "• 음성 데이터는 Android SpeechRecognizer를 통해 Google 서버에서 처리됩니다.\n" +
                        "• 오프라인 모드(ML Kit)에서는 번역 데이터가 기기 밖으로 전송되지 않습니다.\n" +
                        "• 이미지(영상) 자체는 외부 서버로 전송되지 않습니다.\n" +
                        "• 회화집 데이터는 기기 내 로컬 저장소에만 보관됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun VoiceRecordingWarning() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    "음성 녹음 주의사항",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "음성 통역 기능은 사용자 본인의 발화 입력만을 위한 기능입니다.\n" +
                        "타인의 동의 없이 제3자의 대화를 녹음하는 것은\n" +
                        "통신비밀보호법 제3조에 의해 처벌받을 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}
