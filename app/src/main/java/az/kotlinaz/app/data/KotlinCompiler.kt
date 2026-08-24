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

/** Sınanmış versiyalar — birincisi işləməsə növbətisinə keçilir. */
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
                is Cavab.Xeta -> sonXeta = n.mesaj
            }
        }
        RunResult.Failed(sonXeta)
    }

    private sealed interface Cavab {
        data class Ugur(val body: String) : Cavab
        data class Xeta(val mesaj: String) : Cavab
    }

    private fun sorgu(versiya: String, govde: String): Cavab {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL("$BASE/$versiya/compiler/run?filename=File.kt")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
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
            } else {
                Cavab.Xeta("Server $kod cavabı qaytardı")
            }
        } catch (e: Exception) {
            Cavab.Xeta(e.message ?: e.javaClass.simpleName)
        } finally {
            conn?.disconnect()
        }
    }

    private fun oxu(body: String): RunResult = try {
        val cavab = json.decodeFromString<ApiResponse>(body)

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

    /** `<outStream>` və `<errStream>` etiketlərini açır. */
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
fun neticeniNormallasdir(s: String?): String =
    (s ?: "")
        .replace("\r\n", "\n")
        .split("\n")
        .joinToString("\n") { it.trimEnd() }
        .trim('\n')
