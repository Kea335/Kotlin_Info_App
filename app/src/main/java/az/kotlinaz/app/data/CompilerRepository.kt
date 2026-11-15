package az.kotlinaz.app.data

import kotlinx.coroutines.flow.StateFlow

/**
 * Kod icrasının repository sərhədi. ViewModel-lər yalnız bunu görür —
 * testdə şəbəkəsiz fake ötürülür (T2.2).
 *
 * Tətbiqdə yeganə tətbiq [KotlinCompiler]-dir (JetBrains kompilyator xidməti).
 */
interface CompilerRepository {
    /** Aktiv Kotlin versiyası — Meydan zolağında və Tənzimləmələrdə göstərilir. */
    val veziyyet: StateFlow<KompilyatorVeziyyeti>

    /** Başlanğıcda bir dəfə: versiya keşini diskdən oxuyur, şəbəkəyə çıxmır. */
    suspend fun hazirla()

    /** Kodu işlədir. Heç vaxt istisna atmır — hər hal [RunResult]-a çevrilir. */
    suspend fun isle(kod: String): RunResult
}
