package az.kotlinaz.app.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Şəbəkəsiz kompilyator: verilən [RunResult]-ı qaytarır, çağırışları sayır.
 * [dayandir] verilsə `isle` o tamamlanana qədər asılı qalır — «icra gedərkən»
 * halını sınamaq üçün.
 */
class FakeCompilerRepository(
    var netice: RunResult = RunResult.Ok(""),
    private val dayandir: CompletableDeferred<Unit>? = null
) : CompilerRepository {
    override val veziyyet: StateFlow<KompilyatorVeziyyeti> =
        MutableStateFlow(KompilyatorVeziyyeti("2.4.10", serverden = false))

    var hazirlaSayi = 0
        private set
    val gonderilenKodlar = mutableListOf<String>()
    val cagirisSayi get() = gonderilenKodlar.size

    override suspend fun hazirla() { hazirlaSayi++ }

    override suspend fun isle(kod: String): RunResult {
        gonderilenKodlar += kod
        dayandir?.await()
        return netice
    }
}
