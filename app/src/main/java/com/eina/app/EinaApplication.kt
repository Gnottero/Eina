package com.eina.app

import android.app.Application
import com.eina.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class EinaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@EinaApplication)
            modules(appModule)
        }
    }
}
