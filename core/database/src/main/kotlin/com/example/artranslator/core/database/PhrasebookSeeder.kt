package com.example.artranslator.core.database

import com.example.artranslator.core.database.dao.PhraseDao
import com.example.artranslator.core.database.entity.PhraseEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds the phrasebook database with default travel phrases for English and Japanese.
 * Called once after a language pack is downloaded.
 */
@Singleton
class PhrasebookSeeder @Inject constructor(
    private val phraseDao: PhraseDao
) {
    fun seedLanguage(languageCode: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val phrases = when (languageCode) {
                "en" -> englishPhrases()
                "ja" -> japanesePhrases()
                "fr" -> frenchPhrases()
                "zh" -> chinesePhrases()
                "de" -> germanPhrases()
                "es" -> spanishPhrases()
                else -> emptyList()
            }
            phraseDao.insertPhrases(phrases)
        }
    }

    private fun englishPhrases() = listOf(
        PhraseEntity(0, "en", "greeting", "안녕하세요", "Hello", "hɛˈloʊ", 1),
        PhraseEntity(0, "en", "greeting", "감사합니다", "Thank you", "θæŋk juː", 2),
        PhraseEntity(0, "en", "greeting", "죄송합니다", "I'm sorry", "aɪm ˈsɒri", 3),
        PhraseEntity(0, "en", "greeting", "잘 지내세요?", "How are you?", "haʊ ɑːr juː", 4),
        PhraseEntity(0, "en", "greeting", "좋습니다", "I'm fine", "aɪm faɪn", 5),
        PhraseEntity(0, "en", "greeting", "만나서 반갑습니다", "Nice to meet you", "naɪs tuː miːt juː", 6),
        PhraseEntity(0, "en", "greeting", "안녕히 가세요", "Goodbye", "ɡʊdˈbaɪ", 7),
        PhraseEntity(0, "en", "restaurant", "얼마예요?", "How much is it?", "haʊ mʌtʃ ɪz ɪt", 1),
        PhraseEntity(0, "en", "restaurant", "메뉴판 주세요", "Menu, please", "ˈmɛnjuː pliːz", 2),
        PhraseEntity(0, "en", "restaurant", "물 주세요", "Water, please", "ˈwɔːtər pliːz", 3),
        PhraseEntity(0, "en", "restaurant", "계산해 주세요", "Check, please", "tʃɛk pliːz", 4),
        PhraseEntity(0, "en", "restaurant", "맛있어요", "It's delicious", "ɪts dɪˈlɪʃəs", 5),
        PhraseEntity(0, "en", "accommodation", "체크인 하겠습니다", "I'd like to check in", "aɪd laɪk tuː tʃɛk ɪn", 1),
        PhraseEntity(0, "en", "accommodation", "체크아웃 하겠습니다", "I'd like to check out", "aɪd laɪk tuː tʃɛk aʊt", 2),
        PhraseEntity(0, "en", "accommodation", "예약했습니다", "I have a reservation", "aɪ hæv ə ˌrɛzərˈveɪʃən", 3),
        PhraseEntity(0, "en", "transport", "공항에 가고 싶어요", "I want to go to the airport", "aɪ wɒnt tuː ɡoʊ tuː ðə ˈɛrpɔːrt", 1),
        PhraseEntity(0, "en", "transport", "지하철역이 어디예요?", "Where is the subway station?", "wɛr ɪz ðə ˈsʌbweɪ ˈsteɪʃən", 2),
        PhraseEntity(0, "en", "transport", "왼쪽", "Turn left", "tɜːrn lɛft", 3),
        PhraseEntity(0, "en", "transport", "오른쪽", "Turn right", "tɜːrn raɪt", 4),
        PhraseEntity(0, "en", "emergency", "도와주세요!", "Help!", "hɛlp", 1),
        PhraseEntity(0, "en", "emergency", "경찰을 불러주세요", "Call the police", "kɔːl ðə pəˈliːs", 2),
        PhraseEntity(0, "en", "emergency", "병원이 어디예요?", "Where is the hospital?", "wɛr ɪz ðə ˈhɒspɪtl", 3),
        PhraseEntity(0, "en", "numbers", "하나", "One", "wʌn", 1),
        PhraseEntity(0, "en", "numbers", "둘", "Two", "tuː", 2),
        PhraseEntity(0, "en", "numbers", "셋", "Three", "θriː", 3),
        PhraseEntity(0, "en", "numbers", "넷", "Four", "fɔːr", 4),
        PhraseEntity(0, "en", "numbers", "다섯", "Five", "faɪv", 5),
        PhraseEntity(0, "en", "numbers", "열", "Ten", "tɛn", 10),
    )

    private fun japanesePhrases() = listOf(
        PhraseEntity(0, "ja", "greeting", "안녕하세요", "こんにちは", "Konnichiwa", 1),
        PhraseEntity(0, "ja", "greeting", "감사합니다", "ありがとうございます", "Arigatou gozaimasu", 2),
        PhraseEntity(0, "ja", "greeting", "죄송합니다", "すみません", "Sumimasen", 3),
        PhraseEntity(0, "ja", "greeting", "잘 부탁드립니다", "よろしくお願いします", "Yoroshiku onegaishimasu", 4),
        PhraseEntity(0, "ja", "greeting", "안녕히 계세요", "さようなら", "Sayounara", 5),
        PhraseEntity(0, "ja", "restaurant", "얼마예요?", "いくらですか？", "Ikura desu ka?", 1),
        PhraseEntity(0, "ja", "restaurant", "맛있어요", "おいしいです", "Oishii desu", 2),
        PhraseEntity(0, "ja", "restaurant", "메뉴 주세요", "メニューをください", "Menyu wo kudasai", 3),
        PhraseEntity(0, "ja", "restaurant", "계산해 주세요", "お会計をお願いします", "Okaikei wo onegaishimasu", 4),
        PhraseEntity(0, "ja", "accommodation", "체크인 하겠습니다", "チェックインをお願いします", "Chekku-in wo onegaishimasu", 1),
        PhraseEntity(0, "ja", "accommodation", "체크아웃 하겠습니다", "チェックアウトをお願いします", "Chekku-auto wo onegaishimasu", 2),
        PhraseEntity(0, "ja", "transport", "역이 어디예요?", "駅はどこですか？", "Eki wa doko desu ka?", 1),
        PhraseEntity(0, "ja", "transport", "도쿄역까지 가주세요", "東京駅まで行ってください", "Tokyo-eki made itte kudasai", 2),
        PhraseEntity(0, "ja", "emergency", "도와주세요!", "助けてください！", "Tasukete kudasai!", 1),
        PhraseEntity(0, "ja", "emergency", "경찰을 불러주세요", "警察を呼んでください", "Keisatsu wo yonde kudasai", 2),
        PhraseEntity(0, "ja", "numbers", "하나", "一 (いち)", "Ichi", 1),
        PhraseEntity(0, "ja", "numbers", "둘", "二 (に)", "Ni", 2),
        PhraseEntity(0, "ja", "numbers", "셋", "三 (さん)", "San", 3),
        PhraseEntity(0, "ja", "numbers", "열", "十 (じゅう)", "Juu", 10),
    )

    private fun frenchPhrases() = listOf(
        PhraseEntity(0, "fr", "greeting", "안녕하세요", "Bonjour", "bɔ̃ʒuʁ", 1),
        PhraseEntity(0, "fr", "greeting", "감사합니다", "Merci beaucoup", "mɛʁsi boku", 2),
        PhraseEntity(0, "fr", "greeting", "죄송합니다", "Excusez-moi", "ɛkskyze mwa", 3),
        PhraseEntity(0, "fr", "restaurant", "얼마예요?", "Combien ça coûte?", "kɔ̃bjɛ̃ sa kut", 1),
        PhraseEntity(0, "fr", "restaurant", "메뉴 주세요", "Le menu, s'il vous plaît", "lə məny sil vu plɛ", 2),
        PhraseEntity(0, "fr", "emergency", "도와주세요!", "Au secours!", "o skuʁ", 1),
    )

    private fun chinesePhrases() = listOf(
        PhraseEntity(0, "zh", "greeting", "안녕하세요", "你好", "Nǐ hǎo", 1),
        PhraseEntity(0, "zh", "greeting", "감사합니다", "谢谢", "Xiè xiè", 2),
        PhraseEntity(0, "zh", "greeting", "죄송합니다", "对不起", "Duì bu qǐ", 3),
        PhraseEntity(0, "zh", "restaurant", "얼마예요?", "多少钱？", "Duō shao qián?", 1),
        PhraseEntity(0, "zh", "restaurant", "맛있어요", "很好吃", "Hěn hào chī", 2),
        PhraseEntity(0, "zh", "emergency", "도와주세요!", "救命！", "Jiù mìng!", 1),
    )

    private fun germanPhrases() = listOf(
        PhraseEntity(0, "de", "greeting", "안녕하세요", "Guten Tag", "ˈɡuːtən taːk", 1),
        PhraseEntity(0, "de", "greeting", "감사합니다", "Danke schön", "ˈdaŋkə ʃøːn", 2),
        PhraseEntity(0, "de", "greeting", "죄송합니다", "Entschuldigung", "ɛntˈʃʊldɪɡʊŋ", 3),
        PhraseEntity(0, "de", "restaurant", "얼마예요?", "Wie viel kostet das?", "viː fiːl ˈkɔstət das", 1),
        PhraseEntity(0, "de", "emergency", "도와주세요!", "Hilfe!", "ˈhɪlfə", 1),
    )

    private fun spanishPhrases() = listOf(
        PhraseEntity(0, "es", "greeting", "안녕하세요", "Hola", "ˈola", 1),
        PhraseEntity(0, "es", "greeting", "감사합니다", "Gracias", "ˈɡɾasjas", 2),
        PhraseEntity(0, "es", "greeting", "죄송합니다", "Lo siento", "lo ˈsjento", 3),
        PhraseEntity(0, "es", "restaurant", "얼마예요?", "¿Cuánto cuesta?", "ˈkwanto ˈkwesta", 1),
        PhraseEntity(0, "es", "emergency", "도와주세요!", "¡Socorro!", "soˈkoro", 1),
    )
}
