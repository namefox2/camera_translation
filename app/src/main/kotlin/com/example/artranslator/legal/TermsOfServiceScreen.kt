package com.example.artranslator.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("서비스 이용약관") },
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
                AR Translator 서비스 이용약관
                시행일: 2026년 5월 27일
                """.trimIndent()
            )

            PolicySection("제1조 (목적)") {
                PolicyText(
                    "이 약관은 AR Translator(이하 '회사')가 제공하는 AR 실시간 번역 서비스(이하 '서비스')의 이용과 관련하여 회사와 이용자 간의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다."
                )
            }

            PolicySection("제2조 (약관의 효력 및 변경)") {
                PolicyText(
                    """
                    ① 이 약관은 서비스를 이용하고자 하는 모든 이용자에 대하여 그 효력을 발생합니다.
                    ② 회사는 합리적인 사유가 있는 경우 약관을 변경할 수 있으며, 변경 시 앱 공지사항을 통해 7일 전 고지합니다.
                    ③ 이용자가 변경된 약관에 동의하지 않을 경우 서비스 이용을 중단하고 앱을 삭제하실 수 있습니다.
                    """.trimIndent()
                )
            }

            PolicySection("제3조 (서비스의 제공 및 변경)") {
                PolicyText(
                    """
                    ① 회사는 다음 서비스를 제공합니다.
                    • AR 카메라 실시간 번역 서비스
                    • 텍스트 번역 서비스
                    • 음성 통역 서비스
                    • 오프라인 여행 회화집 서비스
                    • 언어팩 다운로드 서비스

                    ② 서비스는 연중무휴 24시간 제공을 원칙으로 하되, 시스템 점검·장애·Google API 정책 변경 등으로 중단될 수 있습니다.
                    ③ 회사는 서비스 내용을 변경할 경우 앱 내 공지사항을 통해 사전에 고지합니다.
                    """.trimIndent()
                )
            }

            PolicySection("제4조 (서비스 이용 시 주의사항 및 금지행위)") {
                PolicyText(
                    """
                    이용자는 다음 행위를 해서는 안 됩니다.

                    ① 타인 권리 침해
                    • 타인의 초상권, 저작권, 개인정보를 무단으로 촬영·번역·배포하는 행위
                    • 타인의 동의 없이 대화를 녹음하는 행위 (통신비밀보호법 위반)

                    ② 불법·유해 이용
                    • 범죄 목적 또는 불법 정보 수집·배포를 위한 번역 서비스 이용
                    • 스팸·광고성 내용을 대량 번역하여 배포하는 행위

                    ③ 시스템 훼손
                    • 서비스를 리버스 엔지니어링하거나 API 키를 무단 추출·사용하는 행위
                    • 자동화 도구로 번역 API를 과도하게 호출하는 행위

                    ④ 위반 시 서비스 이용이 제한될 수 있으며, 관련 법령에 따라 처벌받을 수 있습니다.
                    """.trimIndent()
                )
            }

            PolicySection("제5조 (음성 녹음 관련 특별 약관)") {
                PolicyText(
                    """
                    ① 음성 통역 기능은 이용자 본인의 발화를 입력하는 용도로만 제공됩니다.

                    ② 이용자는 타인의 동의 없이 제3자의 음성을 녹음·번역하지 않을 것에 동의합니다.

                    ③ 「통신비밀보호법」 제3조에 따라 공개되지 않은 타인 간의 대화를 녹음하는 것은 형사처벌 대상입니다.
                    (3년 이하의 징역 또는 1천만 원 이하의 벌금)

                    ④ 이용자가 본 조를 위반하여 발생한 모든 법적 책임은 이용자 본인에게 있습니다.
                    """.trimIndent()
                )
            }

            PolicySection("제6조 (번역 서비스 품질 및 책임 한계)") {
                PolicyText(
                    """
                    ① 번역 서비스는 AI 기반으로 제공되므로 번역 오류가 발생할 수 있습니다.

                    ② 회사는 다음 사항에 대해 책임을 지지 않습니다.
                    • 번역 오류로 인한 의사소통 문제 또는 손해
                    • 의료, 법률, 금융 등 전문 분야에서 번역 결과를 신뢰하여 발생한 손해
                    • Google API 서비스 중단으로 인한 서비스 이용 불가

                    ③ 중요한 계약·의료·법률 관련 번역은 전문 번역사를 이용하시기 바랍니다.
                    """.trimIndent()
                )
            }

            PolicySection("제7조 (지식재산권)") {
                PolicyText(
                    """
                    ① 서비스에 포함된 소프트웨어, UI 디자인, 회화집 데이터에 대한 지식재산권은 회사에 귀속됩니다.

                    ② 이용자가 서비스를 통해 생성한 번역 결과물의 저작권은 이용자에게 귀속됩니다.

                    ③ 이용자는 서비스를 개인적·비영리적 목적으로만 이용할 수 있습니다.
                    """.trimIndent()
                )
            }

            PolicySection("제8조 (준거법 및 관할)") {
                PolicyText(
                    """
                    ① 이 약관은 대한민국 법률에 따라 해석됩니다.

                    ② 서비스 이용으로 발생한 분쟁에 대해 소송이 제기되는 경우 대한민국 법원을 관할 법원으로 합니다.
                    """.trimIndent()
                )
            }

            PolicySection("제9조 (문의처)") {
                PolicyText(
                    """
                    서비스 이용 관련 문의사항은 아래로 연락해 주시기 바랍니다.

                    이메일: namefox2@naver.com
                    처리 기간: 영업일 기준 3일 이내

                    시행일: 2026년 5월 27일
                    """.trimIndent()
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
