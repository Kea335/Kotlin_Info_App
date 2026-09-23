package az.kotlinaz.app.data

import android.content.Context
import androidx.room.Room
import az.kotlinaz.app.data.oyrenme.KotlinAzDatabase
import az.kotlinaz.app.data.oyrenme.OyrenmeRepository
import az.kotlinaz.app.data.oyrenme.RoomOyrenmeRepository

/**
 * Tətbiqin tək asılılıq qrafı. Repozitoriyalar burada bir dəfə yaranır —
 * aktiv keşi ([AssetContentRepository]) və DataStore nüsxəsi ([UserPrefs])
 * bütün ViewModel-lər arasında paylaşılır.
 *
 * Hilt yoxdur: tək modullu tətbiqdə üç asılılıq üçün annotasiya emalı ödəmir.
 * `Context` bu sinifdən kənara çıxmır — ViewModel-lər yalnız hazır obyekt alır.
 */
class AppContainer(context: Context) {
    private val app = context.applicationContext

    val contentRepository: ContentRepository by lazy { AssetContentRepository(app) }

    val userPrefs: UserPrefs by lazy { UserPrefs(app.kotlinazDataStore, kesStore = app.kesDataStore) }

    val compiler: CompilerRepository by lazy { KotlinCompiler(userPrefs, sebekeVar = { app.internetVar() }) }

    /** Öyrənmə tarixçəsi (T5.1) — ilk müraciətdə açılır, başlanğıcı yavaşlatmır. */
    private val database: KotlinAzDatabase by lazy {
        Room.databaseBuilder(app, KotlinAzDatabase::class.java, KotlinAzDatabase.FAYL).build()
    }

    val oyrenme: OyrenmeRepository by lazy { RoomOyrenmeRepository(database) }

    /**
     * Cihazın dil siyahısı, üstünlük sırası ilə — «Sistem dili» seçimində
     * məzmun qovluğu bundan çözülür (T3.3). Lambda-dır: dil dəyişəndə
     * konfiqurasiya yenilənir, hər çağırışda təzə oxunmalıdır.
     * Application kontekstində 13+-da per-app locale də görünür.
     */
    val sistemDilleri: () -> List<String> = {
        app.resources.configuration.locales.toLanguageTags().split(',').filter { it.isNotBlank() }
    }
}
