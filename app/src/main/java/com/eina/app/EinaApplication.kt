package com.eina.app

import android.app.Application
import com.eina.app.data.seed.ExerciseSeeder
import com.eina.app.di.appModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class EinaApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@EinaApplication)
            modules(appModule)
        }

        applicationScope.launch {
            get<ExerciseSeeder>().seedIfEmpty()
        }
    }
}
