package com.letsgo.translator.core.database

import com.letsgo.translator.core.database.dao.PhraseDao
import com.letsgo.translator.core.database.entity.PhraseEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds the phrasebook database with travel phrases after a language pack is downloaded.
 *
 * 기본 8개 항목 (모든 언어 공통):
 *  1. 안녕하세요    → greeting
 *  2. 죄송합니다    → greeting
 *  3. 감사합니다    → greeting
 *  4. 얼마예요?     → restaurant
 *  5. 주세요        → restaurant
 *  6. 숫자 1–10     → numbers
 *  7. 체크인/아웃   → accommodation
 *  8. 짐 맡길 수 있나요? → accommodation
 */
@Singleton
class PhrasebookSeeder @Inject constructor(
    private val phraseDao: PhraseDao
) {
    fun seedLanguage(languageCode: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val phrases = when (languageCode) {
                "en"    -> englishPhrases()
                "ja"    -> japanesePhrases()
                "fr"    -> frenchPhrases()
                "zh","zh-TW" -> chinesePhrases()
                "de"    -> germanPhrases()
                "es"    -> spanishPhrases()
                else    -> emptyList()
            }
            phraseDao.insertPhrases(phrases)
        }
    }

    // ── 영어 ──────────────────────────────────────────────────────────────────

    private fun englishPhrases() = listOf(
        // 인사
        PhraseEntity(0, "en", "greeting", "안녕하세요",          "Hello",                 "hɛˈloʊ",                   1),
        PhraseEntity(0, "en", "greeting", "감사합니다",          "Thank you",             "θæŋk juː",                 2),
        PhraseEntity(0, "en", "greeting", "죄송합니다",          "I'm sorry",             "aɪm ˈsɒri",                3),
        PhraseEntity(0, "en", "greeting", "잘 지내세요?",        "How are you?",          "haʊ ɑːr juː",              4),
        PhraseEntity(0, "en", "greeting", "만나서 반갑습니다",   "Nice to meet you",      "naɪs tuː miːt juː",        5),
        PhraseEntity(0, "en", "greeting", "안녕히 가세요",       "Goodbye",               "ɡʊdˈbaɪ",                  6),
        // 식당·쇼핑
        PhraseEntity(0, "en", "restaurant", "얼마예요?",         "How much is it?",       "haʊ mʌtʃ ɪz ɪt",          1),
        PhraseEntity(0, "en", "restaurant", "주세요",            "This one, please",      "ðɪs wʌn pliːz",            2),
        PhraseEntity(0, "en", "restaurant", "메뉴 주세요",       "Menu, please",          "ˈmɛnjuː pliːz",            3),
        PhraseEntity(0, "en", "restaurant", "물 주세요",         "Water, please",         "ˈwɔːtər pliːz",            4),
        PhraseEntity(0, "en", "restaurant", "계산해 주세요",     "Check, please",         "tʃɛk pliːz",               5),
        PhraseEntity(0, "en", "restaurant", "맛있어요",          "It's delicious",        "ɪts dɪˈlɪʃəs",             6),
        // 숙소
        PhraseEntity(0, "en", "accommodation", "체크인 할게요",  "I'd like to check in",  "aɪd laɪk tuː tʃɛk ɪn",    1),
        PhraseEntity(0, "en", "accommodation", "체크아웃 할게요","I'd like to check out", "aɪd laɪk tuː tʃɛk aʊt",   2),
        PhraseEntity(0, "en", "accommodation", "짐 맡길 수 있나요?", "Can I store my luggage?", "kæn aɪ stɔːr maɪ ˈlʌɡɪdʒ", 3),
        PhraseEntity(0, "en", "accommodation", "예약했습니다",   "I have a reservation",  "aɪ hæv ə ˌrɛzərˈveɪʃən", 4),
        // 교통
        PhraseEntity(0, "en", "transport", "공항에 가주세요",    "To the airport, please","tuː ðə ˈɛrpɔːrt pliːz",    1),
        PhraseEntity(0, "en", "transport", "지하철역이 어디예요?","Where is the subway?", "wɛr ɪz ðə ˈsʌbweɪ",       2),
        PhraseEntity(0, "en", "transport", "왼쪽",               "Turn left",             "tɜːrn lɛft",               3),
        PhraseEntity(0, "en", "transport", "오른쪽",             "Turn right",            "tɜːrn raɪt",               4),
        // 긴급
        PhraseEntity(0, "en", "emergency", "도와주세요!",        "Help!",                 "hɛlp",                     1),
        PhraseEntity(0, "en", "emergency", "경찰 불러주세요",    "Call the police",       "kɔːl ðə pəˈliːs",          2),
        PhraseEntity(0, "en", "emergency", "병원이 어디예요?",   "Where is the hospital?","wɛr ɪz ðə ˈhɒspɪtl",       3),
        // 숫자 1–10
        PhraseEntity(0, "en", "numbers", "하나",  "One",   "wʌn",      1),
        PhraseEntity(0, "en", "numbers", "둘",    "Two",   "tuː",      2),
        PhraseEntity(0, "en", "numbers", "셋",    "Three", "θriː",     3),
        PhraseEntity(0, "en", "numbers", "넷",    "Four",  "fɔːr",     4),
        PhraseEntity(0, "en", "numbers", "다섯",  "Five",  "faɪv",     5),
        PhraseEntity(0, "en", "numbers", "여섯",  "Six",   "sɪks",     6),
        PhraseEntity(0, "en", "numbers", "일곱",  "Seven", "ˈsɛvən",   7),
        PhraseEntity(0, "en", "numbers", "여덟",  "Eight", "eɪt",      8),
        PhraseEntity(0, "en", "numbers", "아홉",  "Nine",  "naɪn",     9),
        PhraseEntity(0, "en", "numbers", "열",    "Ten",   "tɛn",     10),
    )

    // ── 일본어 ────────────────────────────────────────────────────────────────

    private fun japanesePhrases() = listOf(
        // 인사
        PhraseEntity(0, "ja", "greeting", "안녕하세요",          "こんにちは",                   "Konnichiwa",              1),
        PhraseEntity(0, "ja", "greeting", "감사합니다",          "ありがとうございます",          "Arigatou gozaimasu",      2),
        PhraseEntity(0, "ja", "greeting", "죄송합니다",          "すみません",                   "Sumimasen",               3),
        PhraseEntity(0, "ja", "greeting", "잘 부탁드립니다",     "よろしくお願いします",          "Yoroshiku onegaishimasu", 4),
        PhraseEntity(0, "ja", "greeting", "안녕히 계세요",       "さようなら",                   "Sayounara",               5),
        // 식당·쇼핑
        PhraseEntity(0, "ja", "restaurant", "얼마예요?",         "いくらですか？",               "Ikura desu ka?",          1),
        PhraseEntity(0, "ja", "restaurant", "주세요",            "これをください",               "Kore wo kudasai",         2),
        PhraseEntity(0, "ja", "restaurant", "메뉴 주세요",       "メニューをください",           "Menyu wo kudasai",        3),
        PhraseEntity(0, "ja", "restaurant", "계산해 주세요",     "お会計をお願いします",          "Okaikei wo onegaishimasu",4),
        PhraseEntity(0, "ja", "restaurant", "맛있어요",          "おいしいです",                 "Oishii desu",             5),
        // 숙소
        PhraseEntity(0, "ja", "accommodation", "체크인 할게요",  "チェックインをお願いします",   "Chekku-in wo onegaishimasu",  1),
        PhraseEntity(0, "ja", "accommodation", "체크아웃 할게요","チェックアウトをお願いします", "Chekku-auto wo onegaishimasu",2),
        PhraseEntity(0, "ja", "accommodation", "짐 맡길 수 있나요?", "荷物を預かってもらえますか？", "Nimotsu wo azukatte moraemasuka?", 3),
        // 교통
        PhraseEntity(0, "ja", "transport", "역이 어디예요?",     "駅はどこですか？",             "Eki wa doko desu ka?",    1),
        PhraseEntity(0, "ja", "transport", "~까지 가주세요",     "~まで行ってください",          "~made itte kudasai",      2),
        // 긴급
        PhraseEntity(0, "ja", "emergency", "도와주세요!",        "助けてください！",             "Tasukete kudasai!",       1),
        PhraseEntity(0, "ja", "emergency", "경찰 불러주세요",    "警察を呼んでください",          "Keisatsu wo yonde kudasai",2),
        // 숫자 1–10
        PhraseEntity(0, "ja", "numbers", "하나",  "一 (いち)",  "Ichi",    1),
        PhraseEntity(0, "ja", "numbers", "둘",    "二 (に)",    "Ni",      2),
        PhraseEntity(0, "ja", "numbers", "셋",    "三 (さん)",  "San",     3),
        PhraseEntity(0, "ja", "numbers", "넷",    "四 (よん)",  "Yon",     4),
        PhraseEntity(0, "ja", "numbers", "다섯",  "五 (ご)",    "Go",      5),
        PhraseEntity(0, "ja", "numbers", "여섯",  "六 (ろく)",  "Roku",    6),
        PhraseEntity(0, "ja", "numbers", "일곱",  "七 (なな)",  "Nana",    7),
        PhraseEntity(0, "ja", "numbers", "여덟",  "八 (はち)",  "Hachi",   8),
        PhraseEntity(0, "ja", "numbers", "아홉",  "九 (きゅう)","Kyuu",    9),
        PhraseEntity(0, "ja", "numbers", "열",    "十 (じゅう)","Juu",    10),
    )

    // ── 프랑스어 ──────────────────────────────────────────────────────────────

    private fun frenchPhrases() = listOf(
        // 인사
        PhraseEntity(0, "fr", "greeting", "안녕하세요",          "Bonjour",                     "bɔ̃ʒuʁ",                  1),
        PhraseEntity(0, "fr", "greeting", "감사합니다",          "Merci beaucoup",              "mɛʁsi boku",              2),
        PhraseEntity(0, "fr", "greeting", "죄송합니다",          "Excusez-moi",                 "ɛkskyze mwa",             3),
        PhraseEntity(0, "fr", "greeting", "안녕히 가세요",       "Au revoir",                   "o ʁəvwaʁ",                4),
        // 식당·쇼핑
        PhraseEntity(0, "fr", "restaurant", "얼마예요?",         "Combien ça coûte?",           "kɔ̃bjɛ̃ sa kut",           1),
        PhraseEntity(0, "fr", "restaurant", "주세요",            "Ça, s'il vous plaît",         "sa sil vu plɛ",           2),
        PhraseEntity(0, "fr", "restaurant", "메뉴 주세요",       "Le menu, s'il vous plaît",    "lə məny sil vu plɛ",      3),
        PhraseEntity(0, "fr", "restaurant", "계산해 주세요",     "L'addition, s'il vous plaît", "ladisjɔ̃ sil vu plɛ",     4),
        // 숙소
        PhraseEntity(0, "fr", "accommodation", "체크인 할게요",  "Je voudrais m'enregistrer",   "ʒə vudʁɛ mɑ̃ʁɛʒistʁe",   1),
        PhraseEntity(0, "fr", "accommodation", "체크아웃 할게요","Je voudrais régler ma note",  "ʒə vudʁɛ ʁeɡle ma nɔt",  2),
        PhraseEntity(0, "fr", "accommodation", "짐 맡길 수 있나요?", "Puis-je laisser mes bagages?", "pɥi ʒə lɛse me baɡaʒ", 3),
        // 긴급
        PhraseEntity(0, "fr", "emergency", "도와주세요!",        "Au secours!",                 "o skuʁ",                  1),
        PhraseEntity(0, "fr", "emergency", "경찰 불러주세요",    "Appelez la police!",          "aple la pɔlis",           2),
        // 숫자 1–10
        PhraseEntity(0, "fr", "numbers", "하나",  "Un",    "œ̃",    1),
        PhraseEntity(0, "fr", "numbers", "둘",    "Deux",  "dø",   2),
        PhraseEntity(0, "fr", "numbers", "셋",    "Trois", "tʁwa", 3),
        PhraseEntity(0, "fr", "numbers", "넷",    "Quatre","katʁ", 4),
        PhraseEntity(0, "fr", "numbers", "다섯",  "Cinq",  "sɛ̃k", 5),
        PhraseEntity(0, "fr", "numbers", "여섯",  "Six",   "sis",  6),
        PhraseEntity(0, "fr", "numbers", "일곱",  "Sept",  "sɛt",  7),
        PhraseEntity(0, "fr", "numbers", "여덟",  "Huit",  "ɥit",  8),
        PhraseEntity(0, "fr", "numbers", "아홉",  "Neuf",  "nœf",  9),
        PhraseEntity(0, "fr", "numbers", "열",    "Dix",   "dis", 10),
    )

    // ── 중국어 ────────────────────────────────────────────────────────────────

    private fun chinesePhrases() = listOf(
        // 인사
        PhraseEntity(0, "zh", "greeting", "안녕하세요",          "你好",        "Nǐ hǎo",          1),
        PhraseEntity(0, "zh", "greeting", "감사합니다",          "谢谢",        "Xiè xiè",         2),
        PhraseEntity(0, "zh", "greeting", "죄송합니다",          "对不起",      "Duì bu qǐ",       3),
        PhraseEntity(0, "zh", "greeting", "안녕히 가세요",       "再见",        "Zài jiàn",        4),
        // 식당·쇼핑
        PhraseEntity(0, "zh", "restaurant", "얼마예요?",         "多少钱？",    "Duō shao qián?",  1),
        PhraseEntity(0, "zh", "restaurant", "주세요",            "请给我这个",  "Qǐng gěi wǒ zhège",2),
        PhraseEntity(0, "zh", "restaurant", "계산해 주세요",     "买单",        "Mǎi dān",         3),
        PhraseEntity(0, "zh", "restaurant", "맛있어요",          "很好吃",      "Hěn hào chī",     4),
        // 숙소
        PhraseEntity(0, "zh", "accommodation", "체크인 할게요",  "我要办理入住","Wǒ yào bànlǐ rùzhù",  1),
        PhraseEntity(0, "zh", "accommodation", "체크아웃 할게요","我要退房",    "Wǒ yào tuìfáng",      2),
        PhraseEntity(0, "zh", "accommodation", "짐 맡길 수 있나요?", "可以寄存行李吗？", "Kěyǐ jìcún xínglǐ ma?", 3),
        // 긴급
        PhraseEntity(0, "zh", "emergency", "도와주세요!",        "救命！",      "Jiù mìng!",       1),
        PhraseEntity(0, "zh", "emergency", "경찰 불러주세요",    "叫警察！",    "Jiào jǐngchá!",   2),
        // 숫자 1–10
        PhraseEntity(0, "zh", "numbers", "하나",  "一", "Yī",   1),
        PhraseEntity(0, "zh", "numbers", "둘",    "二", "Èr",   2),
        PhraseEntity(0, "zh", "numbers", "셋",    "三", "Sān",  3),
        PhraseEntity(0, "zh", "numbers", "넷",    "四", "Sì",   4),
        PhraseEntity(0, "zh", "numbers", "다섯",  "五", "Wǔ",   5),
        PhraseEntity(0, "zh", "numbers", "여섯",  "六", "Liù",  6),
        PhraseEntity(0, "zh", "numbers", "일곱",  "七", "Qī",   7),
        PhraseEntity(0, "zh", "numbers", "여덟",  "八", "Bā",   8),
        PhraseEntity(0, "zh", "numbers", "아홉",  "九", "Jiǔ",  9),
        PhraseEntity(0, "zh", "numbers", "열",    "十", "Shí", 10),
    )

    // ── 독일어 ────────────────────────────────────────────────────────────────

    private fun germanPhrases() = listOf(
        // 인사
        PhraseEntity(0, "de", "greeting", "안녕하세요",          "Guten Tag",                       "ˈɡuːtən taːk",        1),
        PhraseEntity(0, "de", "greeting", "감사합니다",          "Danke schön",                     "ˈdaŋkə ʃøːn",         2),
        PhraseEntity(0, "de", "greeting", "죄송합니다",          "Entschuldigung",                  "ɛntˈʃʊldɪɡʊŋ",        3),
        PhraseEntity(0, "de", "greeting", "안녕히 가세요",       "Auf Wiedersehen",                 "aʊf ˈviːdɐˌzeːən",    4),
        // 식당·쇼핑
        PhraseEntity(0, "de", "restaurant", "얼마예요?",         "Wie viel kostet das?",            "viː fiːl ˈkɔstət das", 1),
        PhraseEntity(0, "de", "restaurant", "주세요",            "Das, bitte",                      "das ˈbɪtə",            2),
        PhraseEntity(0, "de", "restaurant", "계산해 주세요",     "Die Rechnung, bitte",             "diː ˈʁɛçnʊŋ ˈbɪtə",   3),
        // 숙소
        PhraseEntity(0, "de", "accommodation", "체크인 할게요",  "Ich möchte einchecken",           "ɪç ˈmœçtə ˈaɪnˌtʃɛkən", 1),
        PhraseEntity(0, "de", "accommodation", "체크아웃 할게요","Ich möchte auschecken",           "ɪç ˈmœçtə ˈaʊsˌtʃɛkən",2),
        PhraseEntity(0, "de", "accommodation", "짐 맡길 수 있나요?", "Kann ich mein Gepäck aufbewahren?", "kan ɪç maɪn ɡəˈpɛk ˈaʊfbəˌvaːrən", 3),
        // 긴급
        PhraseEntity(0, "de", "emergency", "도와주세요!",        "Hilfe!",                          "ˈhɪlfə",               1),
        PhraseEntity(0, "de", "emergency", "경찰 불러주세요",    "Rufen Sie die Polizei!",          "ˈʁuːfən ziː diː poliˈtsaɪ", 2),
        // 숫자 1–10
        PhraseEntity(0, "de", "numbers", "하나",  "Eins",   "aɪns",     1),
        PhraseEntity(0, "de", "numbers", "둘",    "Zwei",   "tsvaɪ",    2),
        PhraseEntity(0, "de", "numbers", "셋",    "Drei",   "dʁaɪ",     3),
        PhraseEntity(0, "de", "numbers", "넷",    "Vier",   "fiːɐ̯",    4),
        PhraseEntity(0, "de", "numbers", "다섯",  "Fünf",   "fʏnf",     5),
        PhraseEntity(0, "de", "numbers", "여섯",  "Sechs",  "zɛks",     6),
        PhraseEntity(0, "de", "numbers", "일곱",  "Sieben", "ˈziːbən",  7),
        PhraseEntity(0, "de", "numbers", "여덟",  "Acht",   "axt",      8),
        PhraseEntity(0, "de", "numbers", "아홉",  "Neun",   "nɔʏn",     9),
        PhraseEntity(0, "de", "numbers", "열",    "Zehn",   "tseːn",   10),
    )

    // ── 스페인어 ──────────────────────────────────────────────────────────────

    private fun spanishPhrases() = listOf(
        // 인사
        PhraseEntity(0, "es", "greeting", "안녕하세요",          "Hola",                        "ˈola",                 1),
        PhraseEntity(0, "es", "greeting", "감사합니다",          "Gracias",                     "ˈɡɾasjas",             2),
        PhraseEntity(0, "es", "greeting", "죄송합니다",          "Lo siento",                   "lo ˈsjento",           3),
        PhraseEntity(0, "es", "greeting", "안녕히 가세요",       "Adiós",                       "aˈðjos",               4),
        // 식당·쇼핑
        PhraseEntity(0, "es", "restaurant", "얼마예요?",         "¿Cuánto cuesta?",             "ˈkwanto ˈkwesta",      1),
        PhraseEntity(0, "es", "restaurant", "주세요",            "Esto, por favor",             "ˈesto poɾ faˈβoɾ",     2),
        PhraseEntity(0, "es", "restaurant", "계산해 주세요",     "La cuenta, por favor",        "la ˈkwenta poɾ faˈβoɾ",3),
        // 숙소
        PhraseEntity(0, "es", "accommodation", "체크인 할게요",  "Quiero hacer el check-in",    "ˈkjeɾo aˈθeɾ el ˈtʃekin", 1),
        PhraseEntity(0, "es", "accommodation", "체크아웃 할게요","Quiero hacer el check-out",   "ˈkjeɾo aˈθeɾ el ˈtʃekaut",2),
        PhraseEntity(0, "es", "accommodation", "짐 맡길 수 있나요?", "¿Puedo dejar mi equipaje?","ˈpwedo deˈxaɾ mi ekiˈpaxe",3),
        // 긴급
        PhraseEntity(0, "es", "emergency", "도와주세요!",        "¡Socorro!",                   "soˈkoro",              1),
        PhraseEntity(0, "es", "emergency", "경찰 불러주세요",    "¡Llame a la policía!",        "ˈʎame a la poliˈθia",  2),
        // 숫자 1–10
        PhraseEntity(0, "es", "numbers", "하나",  "Uno",   "ˈuno",     1),
        PhraseEntity(0, "es", "numbers", "둘",    "Dos",   "dos",      2),
        PhraseEntity(0, "es", "numbers", "셋",    "Tres",  "tɾes",     3),
        PhraseEntity(0, "es", "numbers", "넷",    "Cuatro","ˈkwatɾo",  4),
        PhraseEntity(0, "es", "numbers", "다섯",  "Cinco", "ˈsiŋko",   5),
        PhraseEntity(0, "es", "numbers", "여섯",  "Seis",  "seis",     6),
        PhraseEntity(0, "es", "numbers", "일곱",  "Siete", "ˈsjete",   7),
        PhraseEntity(0, "es", "numbers", "여덟",  "Ocho",  "ˈotʃo",    8),
        PhraseEntity(0, "es", "numbers", "아홉",  "Nueve", "ˈnweβe",   9),
        PhraseEntity(0, "es", "numbers", "열",    "Diez",  "ˈdjes",   10),
    )
}
