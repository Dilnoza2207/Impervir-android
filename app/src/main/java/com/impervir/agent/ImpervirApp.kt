package com.impervir.agent

import android.app.Application
import com.impervir.agent.data.ImpervirRepository

class ImpervirApp : Application() {
    /** One app-wide repository. Swap for a real backend later without touching screens. */
    val repository: ImpervirRepository by lazy { ImpervirRepository() }
}
