package az.kotlinaz.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/* ============================================================
   Kotlin kodunun icrası — YEGANƏ internet tələb edən hissə.
   JetBrains-in rəsmi kompilyator xidmətinə müraciət edir;
   saytdakı Kotlin Playground da eyni xidməti işlədir.
   ============================================================ */

private const val BASE = "https://api.kotlinlang.org/api"

/**
 * Sınanmış kompilyator versiyaları. Xidmət ünvandakı versiyanı seçir;
 * biri dəstəklənməsə (server 400/404 qaytarsa) növbətisinə keçilir.
 * Şəbəkə xətasında isə keçid ETMİRİK — bax [KotlinCompiler.isle].
 */
private val VERSIYALAR = listOf("2.1.20", "2.1.0", "2.0.20")

@Serializable
private data class ApiFile(
    val name: String = "File.kt",
    val publicId: String = "",
    val text: String
)

@Serializable
private data class ApiRequest(
    val args: String = "",
    val files: List<ApiFile>,
    val confType: String = "java"
)

@Serializable
private data class ApiInterval(val start: ApiPos? = null, val end: ApiPos? = null)

@Serializable
private data class ApiPos(val line: Int = 0, val ch: Int = 0)

@Serializable
private data class ApiError(
    val interval: ApiInterval? = null,
    val message: String = "",
    val severity: String = "",
    @SerialName("className") val cssClass: String = ""
)

@Serializable
private data class ApiException(
    val message: String? = null,
    val fullName: String = "",
    val cause: ApiException? = null
)

@Serializable
private data class ApiResponse(
    val errors: Map<String, List<ApiError>> = emptyMap(),
    val exception: ApiException? = null,
    val text: String = ""
)

/** İcra nəticəsi. */
sealed interface RunResult {
    /** Kod işlədi — `output` konsol çıxışıdır. */
    data class Ok(val output: String) : RunResult

    /** Kompilyasiya xətaları. */
    data class CompileError(val errors: List<String>) : RunResult

    /** Çalışma vaxtı çökməsi. */
    data class Crashed(val output: String, val exception: String) : RunResult

    /** İnternet yoxdur. */
    data object Offline : RunResult

    /** Şəbəkə və ya server problemi. */
    data class Failed(val message: String) : RunResult
}

class KotlinCompiler(private val context: Context) {

    // encodeDefaults MƏCBURIDIR: onsuz `confType`, `name` və `args` sahələri
    // defolt dəyərli olduqları üçün sorğudan düşür və server sandbox-u çökür.
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /**
     * Şəbəkə var-yoxdur. Yalnız sürətli ilkin yoxlamadır — «şəbəkə var, amma
     * internet yoxdur» halını tutmur, onu artıq sorğunun özü aşkarlayır.
     * ACCESS_NETWORK_STATE icazəsi manifestdə məhz bunun üçündür.
     */
    fun internetVar(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val setler = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return setler.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** Kodu JetBrains kompilyatorunda işlədir. */
    suspend fun isle(kod: String): RunResult = withContext(Dispatchers.IO) {
        if (!internetVar()) return@withContext RunResult.Offline

        val govde = json.encodeToString(
            ApiRequest(files = listOf(ApiFile(text = kod)))
        )

        var sonXeta = "Naməlum xəta"
        for (versiya in VERSIYALAR) {
            when (val n = sorgu(versiya, govde)) {
                is Cavab.Ugur -> return@withContext oxu(n.body)

                // Bu versiya dəstəklənmir — növbətisini sınamağın mənası var.
                is Cavab.Desteklenmir -> sonXeta = n.mesaj

                // DÜZƏLİŞ: əvvəllər HƏR cür uğursuzluq növbəti versiyanı
                // sınamağa aparırdı. Bağlantı kəsiləndə (captive portal, VPN,
                // ölü şəbəkə) bu, 3 × 15 saniyə gözləmə demək idi — istifadəçi
                // təxminən 45 saniyə fırlanğıca baxırdı. Şəbəkə xətasında
                // versiyanı dəyişmək kömək etmir, ona görə dərhal qayıdırıq.
                is Cavab.Xeta -> return@withContext RunResult.Failed(n.mesaj)
            }
        }
        RunResult.Failed(sonXeta)
    }

    private sealed interface Cavab {
        data class Ugur(val body: String) : Cavab

        /** Server sorğunu rədd etdi — versiya dəstəklənmir kimi qiymətləndirilir. */
        data class Desteklenmir(val mesaj: String) : Cavab

        /** Şəbəkə və ya server nasazlığı — versiya dəyişmək kömək etməz. */
        data class Xeta(val mesaj: String) : Cavab
    }

    private fun sorgu(versiya: String, govde: String): Cavab {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL("$BASE/$versiya/compiler/run?filename=File.kt")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                // Qoşulma qısa, oxuma uzun: kompilyasiya + icra server tərəfdə
                // bəzən 10-20 saniyə çəkir, ona görə readTimeout böyükdür.
                connectTimeout = 15_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }
            conn.outputStream.use { it.write(govde.toByteArray(StandardCharsets.UTF_8)) }

            val kod = conn.responseCode
            if (kod in 200..299) {
                val body = conn.inputStream.bufferedReader(StandardCharsets.UTF_8)
                    .use(BufferedReader::readText)
                Cavab.Ugur(body)
            } else if (kod == 400 || kod == 404 || kod == 410) {
                // Ünvandakı versiya tanınmadı — siyahıdakı növbətisini sınamağa dəyər.
                Cavab.Desteklenmir("Server $kod cavabı qaytardı (versiya $versiya)")
            } else {
                // 5xx və digərləri: problem versiyada deyil.
                Cavab.Xeta("Server $kod cavabı qaytardı")
            }
        } catch (e: Exception) {
            // Timeout, DNS, TLS, kəsilmiş bağlantı — hamısı buraya düşür.
            Cavab.Xeta(e.message ?: e.javaClass.simpleName)
        } finally {
            conn?.disconnect()
        }
    }

    private fun oxu(body: String): RunResult = try {
        val cavab = json.decodeFromString<ApiResponse>(body)

        // Xidmət xəbərdarlıqları da `errors` içində qaytarır — yalnız ERROR
        // səviyyəsi kompilyasiyanı sındırır, WARNING göstərilmir.
        val xetalar = cavab.errors.values.flatten()
            .filter { it.severity.equals("ERROR", ignoreCase = true) }
            .map { e ->
                val setir = e.interval?.start?.line?.plus(1)
                if (setir != null) "Sətir $setir: ${e.message}" else e.message
            }

        when {
            xetalar.isNotEmpty() -> RunResult.CompileError(xetalar)

            cavab.exception != null -> {
                val e = cavab.exception
                val ad = e.fullName.ifBlank { "Exception" }
                val mesaj = e.message?.let { ": $it" }.orEmpty()
                RunResult.Crashed(cixisiAyir(cavab.text), ad + mesaj)
            }

            else -> RunResult.Ok(cixisiAyir(cavab.text))
        }
    } catch (e: Exception) {
        RunResult.Failed("Cavab oxunmadı: ${e.message}")
    }

    /**
     * `<outStream>` və `<errStream>` etiketlərini açır.
     *
     * Xidmət etiketlərin içindəki mətni HTML kimi qaçırmır (yoxlanılıb:
     * `println("a & b <tag>")` cavabda olduğu kimi gəlir), ona görə burada
     * heç bir entity açılışı EDİLMİR — əks halda çıxışdakı həqiqi `&lt;`
     * mətni pozulardı.
     */
    private fun cixisiAyir(xam: String): String {
        if (xam.isBlank()) return ""
        val netice = StringBuilder()
        listOf("outStream", "errStream").forEach { etiket ->
            val re = Regex("<$etiket>([\\s\\S]*?)</$etiket>")
            re.findAll(xam).forEach { netice.append(it.groupValues[1]) }
        }
        return if (netice.isEmpty()) xam else netice.toString()
    }
}

/**
 * Nəticə müqayisəsi — saytdakı `normallasdir()` funksiyası ilə eynidir:
 * sətir sonlarını birləşdirir, sağdakı boşluqları və kənar boş sətirləri atır.
 */
// Müqayisə qaydası saytdakı `normallasdir()` ilə hərfi-hərfinə eyni olmalıdır,
// yoxsa saytda keçən həll tətbiqdə keçməzdi.
fun neticeniNormallasdir(s: String?): String =
    (s ?: "")
        .replace("\r\n", "\n")
        .split("\n")
        .joinToString("\n") { it.trimEnd() }
        .trim('\n')
