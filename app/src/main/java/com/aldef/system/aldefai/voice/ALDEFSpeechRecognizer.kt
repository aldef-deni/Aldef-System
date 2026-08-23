package com.aldef.system.aldefai.voice

/** Abstraksi pengenal suara ALDEF AI (agar mesin di baliknya bisa diganti). */
interface ALDEFSpeechRecognizer {
    fun startListening()
    fun stopListening()
    fun cancel()
    fun destroy()
}

/** Pendengar peristiwa pengenalan suara. */
interface ALDEFRecognitionListener {
    fun onState(state: ALDEFAIVoiceState)
    fun onPartial(text: String)
    fun onFinal(text: String)
    fun onRms(level: Float)
}
