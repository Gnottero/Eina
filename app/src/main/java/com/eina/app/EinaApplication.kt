package com.eina.app

import android.app.Application
import android.content.Context
import com.eina.app.data.prefs.AppLocale
import com.eina.app.data.prefs.SettingsRepository
import com.eina.app.data.repository.WorkoutRepository
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

    // The application Context follows the chosen language too: ViewModels read their strings from
    // it, not from the Activity.
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
            // The seed runs off the main thread on first launch: if the asset is missing or
            // malformed the app stays usable with an empty library instead of crashing at start.
            runCatching { get<ExerciseSeeder>().seedIfEmpty() }
            // Ghost rows left by earlier versions: workouts closed without a single completed set,
            // which the history never shows and no screen can delete.
            // See WorkoutRepository.purgeEmptySessions.
            runCatching { get<WorkoutRepository>().purgeEmptySessions() }
            // Once, on the first launch after warmups started counting towards records.
            runCatching {
                if (get<SettingsRepository>().consumePrBackfill()) {
                    get<WorkoutRepository>().recomputeAllPrs()
                }
            }
        }
    }
}
