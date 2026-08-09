package com.eina.app

import android.app.Application
import android.content.Context
import com.eina.app.data.prefs.AppLocale
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

    // Anche il Context dell'applicazione va nella lingua scelta: i ViewModel leggono le
    // stringhe da li', non dall'Activity.
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocale.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@EinaApplication)
            modules(appModule)
        }

        applicationScope.launch {
            // Il seed gira fuori dal main thread al primo avvio: se l'asset manca o e' malformato
            // l'app resta usabile con la libreria vuota invece di crashare in partenza.
            runCatching { get<ExerciseSeeder>().seedIfEmpty() }
        }
    }
}
