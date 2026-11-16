package com.example.wacontactcollector

import android.app.Application
import com.example.wacontactcollector.data.AppContainer

class WaContactApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
