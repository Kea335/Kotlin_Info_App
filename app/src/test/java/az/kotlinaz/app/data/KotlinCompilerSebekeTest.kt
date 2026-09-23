package az.kotlinaz.app.data

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * [KotlinCompiler] lokal HTTP serverə qarşı — real `HttpURLConnection`,
 * JetBrains serveri yoxdur. Versiya siyahısı keşdən gəlir, `/versions`
 * sorğusu getmir.
 */
class KotlinCompilerSebekeTest {

    private lateinit var server: HttpServer
    private val gelenler = CopyOnWriteArrayList<String>()
    /** Cavabı saxlayır — «asılı qalmış sorğu» üçün; testin sonunda buraxılır. */
    private val buraxma = CountDownLatch(1)
    private var cavab: (HttpExchange) -> Unit = { it.jsonYaz(200, "{}") }

    @Before
    fun basla() {
        server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        server.createContext("/api/") { ex ->
            gelenler += ex.requestURI.path + " " + ex.requestBody.readBytes().decodeToString()
            cavab(ex)
        }
        server.executor = java.util.concurrent.Executors.newCachedThreadPool()
        server.start()
    }

    @After
    fun dayan() {
        buraxma.countDown()
        server.stop(0)
    }

    private fun HttpExchange.jsonYaz(kod: Int, json: String) {
        val b = json.encodeToByteArray()
        responseHeaders.add("Content-Type", "application/json")
        sendResponseHeaders(kod, b.size.toLong())
        responseBody.use { it.write(b) }
    }

    private suspend fun kompilyator(): KotlinCompiler {
        val prefs = UserPrefs(YaddasDataStore())
        prefs.kompilyatorKesiniYaz(
            listOf(KompilyatorVersiyasi("2.4.10", latestStable = true), KompilyatorVersiyasi("2.3.21")),
            System.currentTimeMillis()
        )
        val kok = "http://127.0.0.1:${server.address.port}"
        return KotlinCompiler(
            prefs,
            sebekeVar = { true },
            io = Dispatchers.IO,
            apiUrl = "$kok/api",
            versiyalarUrl = "$kok/versions"
        )
    }

    @Test
    fun `ugurlu icra - kod gonderilir, cixis oxunur`() = runBlocking {
        cavab = { it.jsonYaz(200, """{"errors":{"File.kt":[]},"text":"<outStream>salam\n</outStream>"}""") }

        val n = kompilyator().isle("fun main() = println(\"salam\")")

        assertEquals(RunResult.Ok("salam\n"), n)
        assertEquals(1, gelenler.size)
        assertTrue(gelenler[0].contains("/compiler/run"), gelenler[0])
        // encodeDefaults: confType və fayl adı sorğuda olmalıdır, yoxsa server sandbox-u çökür.
        assertTrue(gelenler[0].contains("\"confType\":\"java\""), gelenler[0])
        assertTrue(gelenler[0].contains("\"name\":\"File.kt\""), gelenler[0])
    }

    @Test
    fun `server xetasi - novbeti versiya sinanmir`() = runBlocking {
        cavab = { it.jsonYaz(503, "{}") }

        val n = kompilyator().isle("fun main() {}")

        assertIs<RunResult.Failed>(n)
        assertEquals(1, gelenler.size)
    }

    @Test
    fun `versiya desteklenmir - novbetisi sinanir`() = runBlocking {
        // Birinci namizəd rədd olunur, ikincisi işləyir.
        cavab = { ex ->
            if (gelenler.size == 1) ex.jsonYaz(404, "{}")
            else ex.jsonYaz(200, """{"text":"<outStream>ok</outStream>"}""")
        }

        val n = kompilyator().isle("fun main() {}")

        assertEquals(RunResult.Ok("ok"), n)
        assertEquals(2, gelenler.size)
    }

    @Test
    fun `legv asili qalmis sorgunu derhal kesir`() = runBlocking {
        // Server sorğunu qəbul edir, amma cavab vermir — readTimeout 60 s-dir.
        cavab = { buraxma.await(90, TimeUnit.SECONDS) }
        val k = kompilyator()

        val icra = launch(Dispatchers.IO) { k.isle("while (true) {}") }
        withTimeout(5_000) { while (gelenler.isEmpty()) delay(20) }

        val muddet = measureTimeMillis { withTimeout(5_000) { icra.cancelAndJoin() } }

        assertTrue(icra.isCancelled)
        assertTrue(muddet < 2_000, "ləğv $muddet ms çəkdi")
    }
}
