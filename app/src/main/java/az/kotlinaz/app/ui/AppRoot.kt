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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import az.kotlinaz.app.ui.components.LocalBildiris
import az.kotlinaz.app.ui.components.LocalBolmeyeKec
import az.kotlinaz.app.ui.components.LocalMeydanaGonder
import az.kotlinaz.app.ui.exercises.ExercisePlayerScreen
import az.kotlinaz.app.ui.exercises.ExercisesScreen
import az.kotlinaz.app.ui.lessons.LessonDetailScreen
import az.kotlinaz.app.ui.lessons.LessonsScreen
import az.kotlinaz.app.ui.playground.PlaygroundScreen
import az.kotlinaz.app.ui.quiz.QuizScreen
import az.kotlinaz.app.ui.search.SearchScreen
import az.kotlinaz.app.ui.settings.SettingsScreen
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

    // Kökdə yalnız naviqasiya üçün lazım olan state toplanır (T2.5): yükləmə
    // bayraqları və başlıq/keçid üçün məzmun siyahıları. Qalanını ekranlar
    // öz ViewModel-lərindən özləri oxuyur — bir çalışmanın həll edilməsi
    // naviqasiya kökünü yenidən qurmur.
    val hazir by vm.hazir.collectAsStateWithLifecycle()
    val sections by vm.sections.collectAsStateWithLifecycle()
    val topics by vm.topics.collectAsStateWithLifecycle()
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
                        vm = viewModel(factory = AppViewModelFactory.Factory),
                        appVm = vm,
                        onSection = { nav.navigate(Yol.ders(it)) }
                    )
                }

                composable(Yol.DERS) { entry ->
                    LessonDetailScreen(
                        vm = viewModel(factory = AppViewModelFactory.Factory),
                        appVm = vm,
                        id = entry.arguments?.getString("id").orEmpty(),
                        onNavigate = { nav.navigate(Yol.ders(it)) },
                        onQuiz = { nav.navigate(Yol.QUIZ) },
                        onPlayground = { nav.navigate(Yol.MEYDAN) },
                        onExercises = { nav.navigate(Yol.CALISMALAR) },
                        onMovzu = { nav.navigate(Yol.calisma(it)) }
                    )
                }

                composable(Yol.CALISMALAR) {
                    ExercisesScreen(
                        appVm = vm,
                        onTopic = { nav.navigate(Yol.calisma(it)) }
                    )
                }

                composable(Yol.CALISMA) { entry ->
                    // ViewModel naviqasiya girişinə bağlıdır: mövzu başına
                    // bir nüsxə, geri qayıdanda vəziyyət və gedən sorğu qalır.
                    ExercisePlayerScreen(
                        vm = viewModel(factory = AppViewModelFactory.Factory),
                        appVm = vm,
                        id = entry.arguments?.getString("id").orEmpty()
                    )
                }

                composable(Yol.QUIZ) {
                    // Tur QuizViewModel-dədir (naviqasiya girişinə bağlı) —
                    // proses ölümündən və döndərmədən sağ çıxır.
                    QuizScreen(vm = viewModel(factory = AppViewModelFactory.Factory))
                }

                composable(Yol.MEYDAN) {
                    PlaygroundScreen(
                        vm = viewModel(factory = AppViewModelFactory.Factory),
                        appVm = vm
                    )
                }

                composable(Yol.AXTARIS) {
                    SearchScreen(
                        vm = viewModel(factory = AppViewModelFactory.Factory),
                        onSection = { nav.navigate(Yol.ders(it)) }
                    )
                }

                composable(Yol.TENZIMLEME) {
                    SettingsScreen(
                        vm = viewModel(factory = AppViewModelFactory.Factory),
                        appVm = vm
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
