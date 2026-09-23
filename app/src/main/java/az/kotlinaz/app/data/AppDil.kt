package az.kotlinaz.app.data

import java.util.Locale

/**
 * Tətbiqin dəstəklədiyi dillər (T3.3). `null` seçimi «Sistem dili» deməkdir.
 *
 * [ozAdi] tərcümə OLUNMUR — hər dil öz adı ilə görünür ki, istifadəçi başa
 * düşmədiyi dildəki siyahıda öz dilini tapa bilsin.
 *
 * [teq] BCP-47-dir: həm `AppCompatDelegate.setApplicationLocales`-ə, həm də
 * `assets/content/<teq>/` qovluğuna gedir. Resurs qovluğu adı fərqli ola bilər
 * (`zh-CN` → `values-b+zh+Hans`); onu Android özü çözür.
 */
enum class AppDil(
    val teq: String,
    val ozAdi: String,
    val rtl: Boolean = false
) {
    // i18n-ignore-start — dillərin öz adları (endonim) tərcümə olunmur
    AZ("az", "Azərbaycan"),
    TR("tr", "Türkçe"),
    EN("en", "English"),
    RU("ru", "Русский"),
    ES("es", "Español"),
    PT_BR("pt-BR", "Português (Brasil)"),
    DE("de", "Deutsch"),
    FR("fr", "Français"),
    AR("ar", "العربية", rtl = true),
    FA("fa", "فارسی", rtl = true),
    HI("hi", "हिन्दी"),
    ID("id", "Bahasa Indonesia"),
    ZH_CN("zh-CN", "简体中文"),
    JA("ja", "日本語"),
    KO("ko", "한국어");
    // i18n-ignore-end

    companion object {
        /** Tam teq uyğunluğu; naməlum və ya null → null (sistem dili). */
        fun from(teq: String?): AppDil? = entries.firstOrNull { it.teq == teq }

        /**
         * Teqə ən yaxın dil: əvvəl tam uyğunluq, sonra dil kodu (`en-US` → EN,
         * `pt-PT` → PT_BR, `zh-Hans-CN` → ZH_CN). Sistem tənzimləmələrindən
         * gələn teq bizim yazdığımızdan fərqli formada ola bilər.
         */
        fun yaxin(teq: String?): AppDil? {
            if (teq.isNullOrBlank()) return null
            from(teq)?.let { return it }
            val dil = Locale.forLanguageTag(teq).language
            if (dil.isEmpty()) return null
            return entries.firstOrNull { Locale.forLanguageTag(it.teq).language == dil }
        }
    }
}

/**
 * Məzmun qovluğunun dili: istifadəçinin seçimi (və ya sistem dilləri) ilə
 * mövcud məzmun dillərinin kəsişməsi. Saf funksiyadır — testdə birbaşa çağırılır.
 *
 * Sıra: namizədlərin hər biri üçün əvvəl tam teq (`pt-BR`), sonra dil kodu
 * (`pt-PT` → `pt-BR`, `en-US` → `en`). Heç biri yoxdursa `en`, o da yoxdursa
 * baza (`az`). Tərcüməsi olmayan dildə tətbiq beləcə çökmür, sadəcə
 * bazanı göstərir.
 *
 * @param secim  istifadəçinin seçdiyi teq; null = sistem dili
 * @param sistem cihazın dil siyahısı, üstünlük sırası ilə (`Configuration.locales`)
 * @param movcud `assets/content/locales.json`-dakı qovluqlar
 */
fun mezmunDiliSec(secim: String?, sistem: List<String>, movcud: Collection<String>): String {
    val namizedler = if (secim != null) listOf(secim) else sistem
    for (teq in namizedler) {
        if (teq in movcud) return teq
        val dil = Locale.forLanguageTag(teq).language
        if (dil.isEmpty()) continue
        movcud.firstOrNull { Locale.forLanguageTag(it).language == dil }?.let { return it }
    }
    return if (MEZMUN_FALLBACK in movcud) MEZMUN_FALLBACK else AssetContentRepository.BAZA_LOCALE
}

/** Seçilmiş dildə məzmun yoxdursa əvvəl bu dilə, sonra bazaya düşülür. */
const val MEZMUN_FALLBACK = "en"

/**
 * Tənzimləmələrin dil siyahısı (T4.7). Release-də yalnız məzmunu tam olan
 * dillər görünür — `locales.json`-a `tools/build-locale.js` yalnız 100 %
 * tərcümə olunmuş dilləri yazır, yarımçıq dilin aktivləri debug source
 * set-indədir. Beləcə istifadəçi yarımçıq tərcüməyə keçə bilmir və
 * «dəyişmədi» çaşqınlığı yaranmır.
 *
 * Baza həmişə görünür. Hazırkı seçim də görünür — dil sonradan siyahıdan
 * düşsə belə istifadəçi nəyin seçili olduğunu görüb başqasına keçə bilsin.
 * Debug-da ([hamisi]) bütün dillər: tərcüməçi yarımçıq işini yoxlaya bilsin.
 */
fun gorunenDilleriSec(movcud: Collection<String>, secim: AppDil?, hamisi: Boolean): List<AppDil> =
    if (hamisi) AppDil.entries
    else AppDil.entries.filter {
        it.teq == AssetContentRepository.BAZA_LOCALE || it.teq in movcud || it == secim
    }
