package az.kotlinaz.app.data

import android.content.Context

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

    val userPrefs: UserPrefs by lazy { UserPrefs(app.kotlinazDataStore) }

    // T2.2-də CompilerRepository interfeysi ilə əvəzlənəcək; hələlik konkret sinif.
    val compiler: KotlinCompiler by lazy { KotlinCompiler(userPrefs, sebekeVar = { app.internetVar() }) }
}
