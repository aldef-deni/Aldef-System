package com.aldef.system.aldefai.voice

/** Status pipeline suara ALDEF AI. */
sealed class ALDEFAIVoiceState {
    data object Idle : ALDEFAIVoiceState()
    data object Listening : ALDEFAIVoiceState()
    data object Processing : ALDEFAIVoiceState()
    data object Speaking : ALDEFAIVoiceState()
    data class Error(val message: String) : ALDEFAIVoiceState()
}
