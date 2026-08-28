package az.kotlinaz.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import az.kotlinaz.app.ui.components.LocalBildiris
import az.kotlinaz.app.ui.components.LocalBolmeyeKec
import az.kotlinaz.app.ui.components.LocalMeydanaGonder
import az.kotlinaz.app.ui.screens.ExercisePlayerScreen
import az.kotlinaz.app.ui.screens.ExercisesScreen
import az.kotlinaz.app.ui.screens.LessonDetailScreen
import az.kotlinaz.app.ui.screens.LessonsScreen
import az.kotlinaz.app.ui.screens.PlaygroundScreen
import az.kotlinaz.app.ui.screens.QuizScreen
import az.kotlinaz.app.ui.screens.SearchScreen
import az.kotlinaz.app.ui.screens.SettingsScreen
import az.kotlinaz.app.ui.theme.KAz
import kotlinx.coroutines.launch

// Naviqasiya ünvanları. `{id}` — NavHost-un parametr sintaksisidir; aşağıdakı
// ders()/calisma() funksiyaları isə konkret ünvanı qurur.
private object Yol {
    const val DERSLER = "dersler"
    const val DERS = "ders/{id}"
    const val CALISMALAR = "calismalar"
    const val CALISMA = "calisma/{id}"
    const val QUIZ = "quiz"
    const val MEYDAN = "meydan"
    const val AXTARIS = "axtaris"
    const val TENZIMLEME = "tenzimleme"

    fun ders(id: String) = "ders/$id"
    fun calisma(id: String) = "calisma/$id"
}

private data class AltMenyu(
    val yol: String,
    val etiket: String,
    val ikon: ImageVector
)

private val ALT_MENYU = listOf(
    AltMenyu(Yol.DERSLER, "Dərslər", Icons.AutoMirrored.Outlined.MenuBook),
    AltMenyu(Yol.CALISMALAR, "Çalışmalar", Icons.Outlined.EditNote),
    AltMenyu(Yol.QUIZ, "Test", Icons.Outlined.School),
    AltMenyu(Yol.MEYDAN, "Meydan", Icons.Outlined.Code)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: AppViewModel) {
    val c = KAz.colors
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val hazir by vm.hazir.collectAsStateWithLifecycle()
    val sections by vm.sections.collectAsStateWithLifecycle()
    val topics by vm.topics.collectAsStateWithLifecycle()
    val quiz by vm.quiz.collectAsStateWithLifecycle()
    val presets by vm.presets.collectAsStateWithLifecycle()
    val hellEdilmis by vm.hellEdilmis.collectAsStateWithLifecycle()
    val quizRekordlari by vm.quizRekordlari.collectAsStateWithLifecycle()
    val sonBolme by vm.sonBolme.collectAsStateWithLifecycle()
    val tema by vm.tema.collectAsStateWithLifecycle()
    val srift by vm.sriftOlcusu.collectAsStateWithLifecycle()
    val tamamlama by vm.kodTamamlama.collectAsStateWithLifecycle()
    // Bölmə mənimsəməsi və səviyyə — çalışmalardan hesablanır, tək mənbədən gəlir.
    val tereqqi by vm.tereqqi.collectAsStateWithLifecycle()
    val meydanKodu by vm.meydanKodu.collectAsStateWithLifecycle()

    val yukleneXetasi by vm.yukleneXetasi.collectAsStateWithLifecycle()

    // Cari ünvan həm başlıq, həm «geri» düyməsi, həm də alt menyunun
    // seçili elementi üçün lazımdır.
    val backStack by nav.currentBackStackEntryAsState()
    val cariYol = backStack?.destination?.route

    // Qısa bildiriş (snackbar) — mətn bloklarından da çağırıla bilsin deyə
    // CompositionLocal vasitəsilə aşağıya ötürülür.
    val bildir: (String) -> Unit = { mesaj ->
        scope.launch { snackbar.showSnackbar(mesaj) }
    }

    // Mətn içindəki `#null-safety` tipli keçidlər. Belə id-li bölmə yoxdursa
    // heç nə etmirik — naməlum ünvana keçib «Bölmə tapılmadı» göstərməkdənsə.
    val bolmeyeKec: (String) -> Unit = { href ->
        val id = href.removePrefix("#")
        if (sections.any { it.id == id }) nav.navigate(Yol.ders(id))
    }

    // Kod kartındakı «Meydan» düyməsi: kod ViewModel-ə qoyulur, sonra meydana keçilir.
    val meydanaGonder: (String) -> Unit = { kod ->
        vm.meydanaGonder(kod)
        nav.navigate(Yol.MEYDAN) { launchSingleTop = true }
    }

    // Aktivlər hələ oxunur — fırlanğıc.
    if (!hazir) {
        Box(
            Modifier.fillMaxSize().background(c.bg),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator(color = c.accent) }
        return
    }

    // Aktivlər oxunmadı. Əvvəllər bu hal sonsuz fırlanğıc kimi görünürdü;
    // indi ən azı səbəbi göstəririk ki, xəta bildirilə bilsin.
    if (yukleneXetasi != null || sections.isEmpty()) {
        Box(
            Modifier.fillMaxSize().background(c.bg).padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Məzmun yüklənmədi",
                    style = MaterialTheme.typography.titleMedium,
                    color = c.text
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = yukleneXetasi ?: "Tətbiqin daxili aktivləri oxunmadı.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textFaint,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    CompositionLocalProvider(
        LocalBildiris provides bildir,
        LocalBolmeyeKec provides bolmeyeKec,
        LocalMeydanaGonder provides meydanaGonder
    ) {
        Scaffold(
            containerColor = c.bg,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = basliqUcun(cariYol, sections, topics, backStack?.arguments?.getString("id")),
                            style = MaterialTheme.typography.titleMedium,
                            color = c.text
                        )
                    },
                    navigationIcon = {
                        if (cariYol !in ALT_MENYU.map { it.yol }) {
                            IconButton(onClick = { nav.popBackStack() }) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Geri",
                                    tint = c.text
                                )
                            }
                        }
                    },
                    actions = {
                        // DÜZƏLİŞ: launchSingleTop olmadan axtarış ikonuna hər toxunuş
                        // yığına yeni bir «axtaris» ekranı qoyurdu — beş dəfə basan
                        // istifadəçi geri qayıtmaq üçün beş dəfə «geri» basmalı olurdu.
                        IconButton(onClick = {
                            nav.navigate(Yol.AXTARIS) { launchSingleTop = true }
                        }) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = "Axtarış",
                                tint = c.textDim,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        IconButton(onClick = {
                            nav.navigate(Yol.TENZIMLEME) { launchSingleTop = true }
                        }) {
                            Icon(
                                Icons.Outlined.Settings,
                                contentDescription = "Tənzimləmələr",
                                tint = c.textDim,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = c.bgElev,
                        titleContentColor = c.text
                    )
                )
            },
            bottomBar = {
                NavigationBar(containerColor = c.bgElev) {
                    ALT_MENYU.forEach { menyu ->
                        val secili = cariYol == menyu.yol
                        NavigationBarItem(
                            selected = secili,
                            onClick = {
                                // Alt menyunun standart davranışı: yığın «dersler»-ə
                                // qədər boşalır, hər tabın sürüşmə mövqeyi isə
                                // saveState/restoreState ilə saxlanılır.
                                nav.navigate(menyu.yol) {
                                    popUpTo(Yol.DERSLER) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    menyu.ikon,
                                    contentDescription = menyu.etiket,
                                    modifier = Modifier.size(21.dp)
                                )
                            },
                            label = { Text(menyu.etiket) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = c.accent,
                                selectedTextColor = c.accent,
                                indicatorColor = c.accentSoft,
                                unselectedIconColor = c.textFaint,
                                unselectedTextColor = c.textFaint
                            )
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = Yol.DERSLER,
                modifier = Modifier
                    // Scaffold-un verdiyi boşluq: yuxarıda panel, aşağıda alt menyu.
                    .padding(padding)
                    // DÜZƏLİŞ (ən gözəçarpanı): enableEdgeToEdge() ilə pəncərə
                    // klaviatura üçün ölçüsünü DƏYİŞMİR — manifestdəki adjustResize
                    // artıq işləmir. Nəticədə kod meydanında və praktiki çalışmada
                    // klaviatura redaktorun üstünü örtürdü.
                    // consumeWindowInsets(padding) yuxarıdakı .padding()-in artıq
                    // «yedirtdiyi» boşluğu qeyd edir, imePadding() isə yalnız
                    // qalan fərqi əlavə edir — yəni alt boşluq ikiqat sayılmır.
                    .consumeWindowInsets(padding)
                    .imePadding()
            ) {
                composable(Yol.DERSLER) {
                    LessonsScreen(
                        sections = sections,
                        tereqqi = tereqqi,
                        sonBolme = sonBolme,
                        onSection = { nav.navigate(Yol.ders(it)) }
                    )
                }

                composable(Yol.DERS) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    val bolme = sections.firstOrNull { it.id == id }
                    if (bolme == null) {
                        BosEkran("Bölmə tapılmadı")
                    } else {
                        // Dərs açılan kimi «son bölmə» yazılır — «Davam et»
                        // kartı yarımçıq qoyulan yerə qaytarsın deyə.
                        LaunchedEffect(bolme.id) { vm.sonBolmeniYaz(bolme.id) }

                        LessonDetailScreen(
                            section = bolme,
                            tereqqi = tereqqi.bolme(bolme.id),
                            // Bölməyə bağlı çalışma mövzusu; JSON-da `sectionId`
                            // sahəsi ilə göstərilir, olmayan bölmələrdə null qalır.
                            movzuId = topics.firstOrNull { it.sectionId == bolme.id }?.id,
                            // `index` content.json-da 0-dan başlayır və siyahıdakı
                            // mövqe ilə eynidir, ona görə qonşular birbaşa götürülür.
                            // Kənarlarda getOrNull null qaytarır — düymə sönük qalır.
                            evvelki = sections.getOrNull(bolme.index - 1),
                            novbeti = sections.getOrNull(bolme.index + 1),
                            onNavigate = { nav.navigate(Yol.ders(it)) },
                            onOxundu = { vm.bolmeniOxunmusIsaretle(it) },
                            onQuiz = { nav.navigate(Yol.QUIZ) },
                            onPlayground = { nav.navigate(Yol.MEYDAN) },
                            onExercises = { nav.navigate(Yol.CALISMALAR) },
                            onMovzu = { nav.navigate(Yol.calisma(it)) }
                        )
                    }
                }

                composable(Yol.CALISMALAR) {
                    ExercisesScreen(
                        topics = topics,
                        hellEdilmis = hellEdilmis,
                        onTopic = { nav.navigate(Yol.calisma(it)) }
                    )
                }

                composable(Yol.CALISMA) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    val movzu = topics.firstOrNull { it.id == id }
                    if (movzu == null) {
                        BosEkran("Mövzu tapılmadı")
                    } else {
                        ExercisePlayerScreen(
                            topic = movzu,
                            hellEdilmis = hellEdilmis,
                            compiler = vm.compiler,
                            onHellIsaretle = { vm.calismaniHellIsaretle(it) }
                        )
                    }
                }

                composable(Yol.QUIZ) {
                    QuizScreen(
                        questions = quiz,
                        // Səviyyə rejimləri sualları çalışma bankından götürür.
                        topics = topics,
                        rekordlar = quizRekordlari,
                        onBitdi = { rejim, bal -> vm.quizNeticesiniYaz(rejim, bal) }
                    )
                }

                composable(Yol.MEYDAN) {
                    PlaygroundScreen(
                        presets = presets,
                        compiler = vm.compiler,
                        xariciKod = meydanKodu,
                        onXariciKodAlindi = { vm.meydanKodunuTemizle() }
                    )
                }

                composable(Yol.AXTARIS) {
                    SearchScreen(
                        axtar = { vm.axtar(it) },
                        onSection = { nav.navigate(Yol.ders(it)) }
                    )
                }

                composable(Yol.TENZIMLEME) {
                    SettingsScreen(
                        tema = tema,
                        sriftOlcusu = srift,
                        kodTamamlama = tamamlama,
                        tereqqi = tereqqi,
                        quizRekordlari = quizRekordlari,
                        onTema = { vm.temaSec(it) },
                        onSrift = { vm.sriftSec(it) },
                        onTamamlama = { vm.tamamlamaSec(it) },
                        onOxumaSifirla = {
                            vm.oxumaTereqqisiniSifirla()
                            bildir("Dərs təsdiqləri sıfırlandı")
                        },
                        onCalismaSifirla = {
                            vm.calismaTereqqisiniSifirla()
                            bildir("Çalışma tərəqqisi sıfırlandı")
                        },
                        onHamisiSifirla = {
                            vm.hamisiniSifirla()
                            bildir("Bütün tərəqqi sıfırlandı")
                        }
                    )
                }
            }
        }
    }
}

// Yuxarı paneldəki başlıq — cari ünvana görə seçilir. Dərs və çalışma
// ekranlarında başlıq məzmundan gəlir, ona görə id lazımdır.
private fun basliqUcun(
    yol: String?,
    sections: List<az.kotlinaz.app.data.model.Section>,
    topics: List<az.kotlinaz.app.data.model.ExerciseTopic>,
    id: String?
): String = when (yol) {
    Yol.DERSLER -> "KotlinAZ"
    Yol.DERS -> sections.firstOrNull { it.id == id }?.title ?: "Dərs"
    Yol.CALISMALAR -> "Çalışmalar"
    Yol.CALISMA -> topics.firstOrNull { it.id == id }?.title ?: "Çalışma"
    Yol.QUIZ -> "Bilik testi"
    Yol.MEYDAN -> "Kod meydanı"
    Yol.AXTARIS -> "Axtarış"
    Yol.TENZIMLEME -> "Tənzimləmələr"
    else -> "KotlinAZ"
}

@Composable
private fun BosEkran(metn: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(metn, color = KAz.colors.textFaint)
    }
}
