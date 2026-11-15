package az.kotlinaz.app

import android.app.Application
import az.kotlinaz.app.data.AppContainer

/**
 * Tətbiqin Application sinfi — asılılıq qrafını ([AppContainer]) bir dəfə
 * yaradır və proses boyu saxlayır. Manifestdə `android:name` ilə qeyd olunub.
 */
class KotlinAzApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
