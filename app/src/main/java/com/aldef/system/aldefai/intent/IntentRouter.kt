package com.aldef.system.aldefai.intent

/**
 * Pengklasifikasi maksud berbasis AI on-device. Berbeda dari [IntentEngine]
 * berbasis aturan, implementasi ini mungkin **belum tersedia** (model belum
 * dipasang), ditandai [isAvailable].
 */
interface AiIntentClassifier : ALDEFIntentClassifier {
    val isAvailable: Boolean
}

/**
 * Placeholder tempat model AI on-device (mis. TFLite / ML Kit) akan dipasang di
 * masa depan. Untuk sekarang selalu "tidak tersedia", jadi [IntentRouter] akan
 * langsung memakai mesin aturan. Tidak ada model besar yang dimuat.
 */
class NoopAiClassifier : AiIntentClassifier {
    override val isAvailable: Boolean = false
    override fun classify(text: String): ALDEFAIIntent = ALDEFAIIntent.Unknown(text)
}

/**
 * Perutean klasifikasi **local-first**:
 *
 *  1. Jika AI on-device tersedia dan cukup yakin → pakai hasil AI.
 *  2. Selain itu → pakai mesin aturan (selalu tersedia, tanpa jaringan).
 *  3. Jika aturan tak mengerti tapi AI punya tebakan → pakai tebakan AI.
 *
 * Panel dan eksekutor cukup tahu [ALDEFIntentClassifier]; mesin di baliknya bisa
 * ditukar tanpa mengubah mereka.
 */
class IntentRouter(
    private val rules: ALDEFIntentClassifier,
    private val ai: AiIntentClassifier? = null,
    private val aiMinConfidence: Float = 0.6f
) : ALDEFIntentClassifier {

    override fun classify(text: String): ALDEFAIIntent {
        val aiResult = ai
            ?.takeIf { it.isAvailable }
            ?.classify(text)
            ?.takeIf { it !is ALDEFAIIntent.Unknown && it.confidence >= aiMinConfidence }
        if (aiResult != null) return aiResult

        val ruleResult = rules.classify(text)
        if (ruleResult !is ALDEFAIIntent.Unknown) return ruleResult

        // Aturan tak mengerti — beri kesempatan tebakan AI (walau di bawah ambang).
        val aiGuess = ai?.takeIf { it.isAvailable }?.classify(text)
        return aiGuess?.takeIf { it !is ALDEFAIIntent.Unknown } ?: ruleResult
    }
}

/** Perakit pengklasifikasi standar ALDEF AI. */
object IntentClassifiers {
    /**
     * Pengklasifikasi bawaan: AI on-device bila tersedia + fallback aturan.
     * Saat ini AI-nya [NoopAiClassifier] (belum ada), jadi efektifnya memakai
     * [IntentEngine].
     */
    fun default(): ALDEFIntentClassifier =
        IntentRouter(rules = IntentEngine(), ai = NoopAiClassifier())
}
