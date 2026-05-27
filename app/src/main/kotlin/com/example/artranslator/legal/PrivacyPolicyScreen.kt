package com.example.artranslator.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 개인정보처리방침 화면
 * 개인정보보호법 제30조, 정보통신망법 제27조의2 준수
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("개인정보처리방침") },
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
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            PolicyText(
                """
                AR Translator(이하 '회사')는 「개인정보보호법」 제30조 및 「정보통신망 이용촉진 및 정보보호 등에 관한 법률」 제27조의2에 따라 이용자의 개인정보를 보호하고 이와 관련한 고충을 신속하고 원활하게 처리할 수 있도록 다음과 같이 개인정보처리방침을 수립·공개합니다.

                시행일: 2026년 5월 27일
                """.trimIndent()
            )

            PolicySection("제1조 (수집하는 개인정보 항목 및 수집 방법)") {
                PolicyText(
                    """
                    ① 회사는 서비스 제공을 위해 아래와 같이 최소한의 정보를 처리합니다.

                    [필수 처리 항목]
                    • 번역 대상 텍스트: AR 카메라 인식 텍스트, 직접 입력 텍스트
                    • 음성 데이터: 음성 통역 기능 사용 시 마이크 버튼 활성화 동안의 발화
                    • 앱 설정 정보: 선택 언어, 테마 설정 (기기 내 저장)

                    [비수집 항목]
                    • 카메라 영상(이미지) 자체는 외부 서버로 전송되지 않습니다.
                    • 회원 가입, 이름, 연락처, 이메일 등 식별 정보를 수집하지 않습니다.

                    ② 수집 방법: 서비스 이용 중 자동 수집
                    """.trimIndent()
                )
            }

            PolicySection("제2조 (개인정보의 처리 목적)") {
                PolicyText(
                    """
                    회사는 다음 목적으로 개인정보를 처리합니다. 처리한 개인정보는 아래의 목적 이외의 용도로는 이용하지 않습니다.

                    • 번역 텍스트: Google Cloud Translation API를 통한 번역 결과 제공
                    • 음성 데이터: Android SpeechRecognizer를 통한 음성 인식 및 번역 제공
                    • 오프라인 데이터: ML Kit 온디바이스 모델을 통한 오프라인 번역 제공
                    """.trimIndent()
                )
            }

            PolicySection("제3조 (개인정보의 제3자 제공)") {
                PolicyText(
                    """
                    ① 회사는 이용자의 개인정보를 제2조의 목적 이외의 용도로 사용하거나 이용자의 동의 없이 제3자에게 제공하지 않습니다.

                    ② 다음의 경우 이용자 정보가 제3자(외부 서비스)에서 처리됩니다.

                    [Google LLC]
                    - 제공 정보: 번역 대상 텍스트, 음성 데이터 (인터넷 연결 시)
                    - 제공 목적: Google Cloud Translation API 번역 서비스 제공, SpeechRecognizer 음성 인식
                    - 보유 기간: Google의 개인정보처리방침에 따름
                    - Google 개인정보처리방침: https://policies.google.com/privacy

                    ③ 오프라인 모드(ML Kit 온디바이스) 사용 시에는 어떠한 데이터도 외부로 전송되지 않습니다.
                    """.trimIndent()
                )
            }

            PolicySection("제4조 (개인정보의 보유·이용 기간)") {
                PolicyText(
                    """
                    ① 번역 텍스트 및 음성 데이터: 번역 결과 반환 즉시 폐기됩니다. 회사는 이를 별도로 저장하지 않습니다.

                    ② 앱 내 설정 정보(언어, 테마): 앱 삭제 시까지 기기 내부에 보관됩니다.

                    ③ 회화집 데이터: 이용자가 직접 언어팩을 삭제하거나 앱을 삭제할 때까지 기기 내부에 보관됩니다.
                    """.trimIndent()
                )
            }

            PolicySection("제5조 (개인정보 처리의 위탁)") {
                PolicyText(
                    """
                    회사는 서비스 이용 과정에서 아래와 같이 개인정보 처리 업무를 위탁하고 있습니다.

                    수탁업체: Google LLC
                    위탁 업무: 텍스트 번역 처리(Cloud Translation API), 음성 인식(SpeechRecognizer)
                    개인정보 보유 기간: 위탁 업무 처리 완료 후 즉시

                    회사는 위탁 계약 시 개인정보보호 관련 법규의 준수, 개인정보에 관한 제3자 제공 금지 등을 규정하고 이를 준수합니다.
                    """.trimIndent()
                )
            }

            PolicySection("제6조 (이용자의 권리·의무 및 행사 방법)") {
                PolicyText(
                    """
                    이용자는 회사에 대해 언제든지 다음 각 호의 권리를 행사할 수 있습니다.

                    1. 개인정보 처리 현황 통지 요구
                    2. 개인정보 처리 정지 요구
                    3. 개인정보 삭제 요구

                    ※ 본 앱은 서버에 개인정보를 별도 저장하지 않으므로, 앱 삭제를 통해 기기 내 모든 데이터를 삭제할 수 있습니다.
                    """.trimIndent()
                )
            }

            PolicySection("제7조 (개인정보의 안전성 확보 조치)") {
                PolicyText(
                    """
                    회사는 개인정보 보호를 위해 다음 조치를 취하고 있습니다.

                    • API 키 암호화 관리: Google Cloud API 키를 소스코드에 포함하지 않고 별도 관리
                    • 최소 수집 원칙: 서비스에 필요한 최소한의 정보만 처리
                    • 전송 구간 보호: HTTPS(TLS)를 통한 외부 API 통신 암호화
                    • 로컬 데이터 보호: Android Keystore 기반 기기 내 데이터 보호
                    """.trimIndent()
                )
            }

            PolicySection("제8조 (개인정보보호 책임자)") {
                PolicyText(
                    """
                    회사는 개인정보 처리에 관한 업무를 총괄해서 책임지고, 이용자의 개인정보 관련 불만 처리 및 피해 구제를 위하여 아래와 같이 개인정보보호 책임자를 지정하고 있습니다.

                    개인정보보호 책임자
                    • 이메일: namefox2@naver.com
                    • 처리 시간: 영업일 기준 3일 이내 답변

                    이용자는 회사의 서비스(또는 사업)을 이용하시면서 발생한 모든 개인정보보호 관련 문의, 불만 처리, 피해 구제 등에 관한 사항을 개인정보보호 책임자에게 문의하실 수 있습니다.
                    """.trimIndent()
                )
            }

            PolicySection("제9조 (개인정보 처리방침의 변경)") {
                PolicyText(
                    """
                    이 개인정보처리방침은 시행일로부터 적용되며, 법령 및 방침에 따른 변경 내용의 추가, 삭제 및 정정이 있는 경우에는 변경사항의 시행 7일 전부터 공지사항을 통하여 고지할 것입니다.

                    시행일: 2026년 5월 27일
                    """.trimIndent()
                )
            }

            PolicySection("제10조 (권익침해 구제방법)") {
                PolicyText(
                    """
                    이용자는 개인정보침해로 인한 피해를 구제받기 위하여 다음 기관에 분쟁 해결이나 상담을 신청하실 수 있습니다.

                    • 개인정보보호위원회: privacy.go.kr / 국번없이 182
                    • 한국인터넷진흥원 개인정보침해신고센터: privacy.kisa.or.kr / 국번없이 118
                    • 대검찰청 사이버수사과: spo.go.kr / 국번없이 1301
                    • 경찰청 사이버수사국: cyberbureau.police.go.kr / 국번없이 182
                    """.trimIndent()
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── 공통 컴포넌트 ─────────────────────────────────────────────────────────────

@Composable
internal fun PolicySection(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(20.dp))
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(8.dp))
    content()
    HorizontalDivider(modifier = Modifier.padding(top = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
internal fun PolicyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        lineHeight = MaterialTheme.typography.bodySmall.fontSize * 1.7,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
    )
}
