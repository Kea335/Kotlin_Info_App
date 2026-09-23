package az.kotlinaz.app.ui

import az.kotlinaz.app.ui.quiz.QuizRejimi
import az.kotlinaz.app.ui.sehvlerim.SehvlerimScreen
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import az.kotlinaz.app.R
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
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import az.kotlinaz.app.data.model.ExerciseTopic
import az.kotlinaz.app.data.model.Section
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

// Alt menyu elementi. `yol` — keçiləcək ünvan, `secilidir` — cari
// destination bu tabdırmı (route tipinə görə, string müqayisəsi yoxdur).
private data class AltMenyu(
    val yol: Any,
    @param:StringRes val etiket: Int,
    val ikon: ImageVector,
    val secilidir: (NavDestination) -> Boolean
)

private val ALT_MENYU = listOf(
    AltMenyu(Yol.Dersler, R.string.nav_dersler, Icons.AutoMirrored.Outlined.MenuBook) { it.hasRoute<Yol.Dersler>() },
    AltMenyu(Yol.Calismalar, R.string.nav_calismalar, Icons.Outlined.EditNote) { it.hasRoute<Yol.Calismalar>() },
    AltMenyu(Yol.Quiz, R.string.nav_test, Icons.Outlined.School) { it.hasRoute<Yol.Quiz>() },
    AltMenyu(Yol.Meydan, R.string.nav_meydan, Icons.Outlined.Code) { it.hasRoute<Yol.Meydan>() }
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
    val cariDest = backStack?.destination
    val altMenyudayiq = cariDest != null && ALT_MENYU.any { it.secilidir(cariDest) }

    // Qısa bildiriş (snackbar) — mətn bloklarından da çağırıla bilsin deyə
    // CompositionLocal vasitəsilə aşağıya ötürülür.
    val bildir: (String) -> Unit = { mesaj ->
        scope.launch { snackbar.showSnackbar(mesaj) }
    }

    // Mətn içindəki `#null-safety` tipli keçidlər. Belə id-li bölmə yoxdursa
    // heç nə etmirik — naməlum ünvana keçib «Bölmə tapılmadı» göstərməkdənsə.
    val bolmeyeKec: (String) -> Unit = { href ->
        val id = href.removePrefix("#")
        if (sections.any { it.id == id }) nav.navigate(Yol.Ders(id))
    }

    // Kod kartındakı «Meydan» düyməsi: kod ViewModel-ə qoyulur, sonra meydana keçilir.
    val meydanaGonder: (String) -> Unit = { kod ->
        vm.meydanaGonder(kod)
        nav.navigate(Yol.Meydan) { launchSingleTop = true }
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
                    text = stringResource(R.string.yukleme_xeta_basliq),
                    style = MaterialTheme.typography.titleMedium,
                    color = c.text
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = yukleneXetasi ?: stringResource(R.string.yukleme_xeta_izah),
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
                            text = basliqUcun(backStack, sections, topics),
                            style = MaterialTheme.typography.titleMedium,
                            color = c.text
                        )
                    },
                    navigationIcon = {
                        if (!altMenyudayiq) {
                            IconButton(onClick = { nav.popBackStack() }) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = stringResource(R.string.nav_geri),
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
                            nav.navigate(Yol.Axtaris) { launchSingleTop = true }
                        }) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = stringResource(R.string.nav_axtaris),
                                tint = c.textDim,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        IconButton(onClick = {
                            nav.navigate(Yol.Tenzimleme) { launchSingleTop = true }
                        }) {
                            Icon(
                                Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.nav_tenzimlemeler),
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
                        val secili = cariDest != null && menyu.secilidir(cariDest)
                        NavigationBarItem(
                            selected = secili,
                            onClick = {
                                // Alt menyunun standart davranışı: yığın «dersler»-ə
                                // qədər boşalır, hər tabın sürüşmə mövqeyi isə
                                // saveState/restoreState ilə saxlanılır.
                                nav.navigate(menyu.yol) {
                                    popUpTo(Yol.Dersler) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    menyu.ikon,
                                    contentDescription = null,
                                    modifier = Modifier.size(21.dp)
                                )
                            },
                            // İkonun təsviri yoxdur — etiket onsuz da oxunur, iki dəfə səslənməsin.
                            label = { Text(stringResource(menyu.etiket)) },
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
                startDestination = Yol.Dersler,
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
                composable<Yol.Dersler> {
                    LessonsScreen(
                        vm = viewModel(factory = AppViewModelFactory.Factory),
                        appVm = vm,
                        onSection = { nav.navigate(Yol.Ders(it)) }
                    )
                }

                composable<Yol.Ders> { entry ->
                    LessonDetailScreen(
                        vm = viewModel(factory = AppViewModelFactory.Factory),
                        appVm = vm,
                        id = entry.toRoute<Yol.Ders>().id,
                        onNavigate = { nav.navigate(Yol.Ders(it)) },
                        onQuiz = { nav.navigate(Yol.Quiz) },
                        onPlayground = { nav.navigate(Yol.Meydan) },
                        onExercises = { nav.navigate(Yol.Calismalar) },
                        onMovzu = { nav.navigate(Yol.Calisma(it)) }
                    )
                }

                composable<Yol.Calismalar> {
                    ExercisesScreen(
                        appVm = vm,
                        onTopic = { nav.navigate(Yol.Calisma(it)) }
                    )
                }

                composable<Yol.Calisma> { entry ->
                    // ViewModel naviqasiya girişinə bağlıdır: mövzu başına
                    // bir nüsxə, geri qayıdanda vəziyyət və gedən sorğu qalır.
                    ExercisePlayerScreen(
                        vm = viewModel(factory = AppViewModelFactory.Factory),
                        appVm = vm,
                        id = entry.toRoute<Yol.Calisma>().id
                    )
                }

                composable<Yol.Quiz> {
                    // Tur QuizViewModel-dədir (naviqasiya girişinə bağlı) —
                    // proses ölümündən və döndərmədən sağ çıxır.
                    QuizScreen(
                        vm = viewModel(factory = AppViewModelFactory.Factory),
                        onSehvlerim = { nav.navigate(Yol.Sehvlerim) { launchSingleTop = true } }
                    )
                }

                composable<Yol.Sehvlerim> {
                    SehvlerimScreen(
                        vm = viewModel(factory = AppViewModelFactory.Factory),
                        onTest = { nav.navigate(Yol.SehvTesti) }
                    )
                }

                composable<Yol.SehvTesti> {
                    QuizScreen(
                        vm = viewModel(factory = AppViewModelFactory.Factory),
                        sabitRejim = QuizRejimi.SEHVLER,
                        onCixis = { nav.popBackStack() },
                        // Səhvlərin siyahısına qayıdış — yeni nüsxə açılmasın.
                        onSehvlerim = { nav.popBackStack() }
                    )
                }

                composable<Yol.Meydan> {
                    PlaygroundScreen(
                        vm = viewModel(factory = AppViewModelFactory.Factory),
                        appVm = vm
                    )
                }

                composable<Yol.Axtaris> {
                    SearchScreen(
                        vm = viewModel(factory = AppViewModelFactory.Factory),
                        onSection = { nav.navigate(Yol.Ders(it)) }
                    )
                }

                composable<Yol.Tenzimleme> {
                    SettingsScreen(
                        vm = viewModel(factory = AppViewModelFactory.Factory),
                        appVm = vm,
                        onSehvlerim = { nav.navigate(Yol.Sehvlerim) { launchSingleTop = true } }
                    )
                }
            }
        }
    }
}

// Yuxarı paneldəki başlıq — cari ünvanın tipinə görə seçilir. Dərs və
// çalışma ekranlarında başlıq məzmundan gəlir; id route-dan tipli oxunur.
@Composable
private fun basliqUcun(
    entry: NavBackStackEntry?,
    sections: List<Section>,
    topics: List<ExerciseTopic>
): String {
    val dest = entry?.destination ?: return stringResource(R.string.app_name)
    return when {
        dest.hasRoute<Yol.Dersler>() -> stringResource(R.string.app_name)
        dest.hasRoute<Yol.Ders>() -> {
            val id = entry.toRoute<Yol.Ders>().id
            sections.firstOrNull { it.id == id }?.title ?: stringResource(R.string.basliq_ders)
        }
        dest.hasRoute<Yol.Calismalar>() -> stringResource(R.string.nav_calismalar)
        dest.hasRoute<Yol.Calisma>() -> {
            val id = entry.toRoute<Yol.Calisma>().id
            topics.firstOrNull { it.id == id }?.title ?: stringResource(R.string.basliq_calisma)
        }
        dest.hasRoute<Yol.Quiz>() -> stringResource(R.string.basliq_bilik_testi)
        dest.hasRoute<Yol.Meydan>() -> stringResource(R.string.basliq_kod_meydani)
        dest.hasRoute<Yol.Axtaris>() -> stringResource(R.string.nav_axtaris)
        dest.hasRoute<Yol.Tenzimleme>() -> stringResource(R.string.nav_tenzimlemeler)
        dest.hasRoute<Yol.Sehvlerim>() -> stringResource(R.string.sehvlerim_basliq)
        dest.hasRoute<Yol.SehvTesti>() -> stringResource(R.string.quiz_rejim_sehvler)
        else -> stringResource(R.string.app_name)
    }
}
