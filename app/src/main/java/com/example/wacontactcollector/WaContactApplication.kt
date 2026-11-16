package com.example.wacontactcollector

import android.app.Application
import androidx.work.Configuration
import com.example.wacontactcollector.data.AppContainer

class WaContactApplication : Application(), Configuration.Provider {
    val container: AppContainer by lazy { AppContainer(this) }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setMinimumLoggingLevel(android.util.Log.INFO).build()
}
