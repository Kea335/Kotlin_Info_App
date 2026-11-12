# KotlinAZ — AI tərcümə prompt şablonu (T4.0)

Hər tərcümə tapşırığı bu şablonla başlayır. `<…>` yerləri doldurulur,
qalan mətn **dəyişmədən** göndərilir. Qaydaların özü
[`POLICY.md`](POLICY.md)-dədir — prompt onu tam daxil edir.

## Necə hazırlanır

1. **Parça seç.** Overlay faylından bir mövzu və ya bölmə
   (`localization/<dil>/exercises.json` → `topics.<id>`). Bir parça
   ≈ 150 sahədən çox olmasın — böyük parçada model sahə atlayır.
2. **Bazanı qoş.** Overlay-də dəyərlər boşdur (`""`); modelə tərcümə
   olunacaq mətni vermək üçün eyni açarlarla bazadakı az mətnini qoy:

   ```
   node -e "const L=require('./tools/content-lib');const ad='exercises';
   const o={};L.YARPAQ[ad](L.bazaOxu(ad)).filter(y=>y.acar[1]==='<MÖVZU>')
   .forEach(y=>L.overlayYaz(o,y.acar,y.deyer));console.log(JSON.stringify(o,null,2))"
   ```

3. **Lüğət parçası.** `glossary.json`-dan yalnız `az` və hədəf dil
   sütunları, üstəlik `doNotTranslate` siyahısı.
4. **Göndər** — aşağıdakı şablonla.
5. **Cavabı overlay-ə yaz** (eyni açarlara) və yoxla:

   ```
   node tools/validate-locales.js <dil>
   node tools/build-locale.js <dil>
   ```

   Xəta varsa — xəta mətnini həmin parça ilə birlikdə modelə qaytar:
   «Bu sahələri düzəlt, qalanına toxunma».

---

## Şablon

````
Sən KotlinAZ öyrənmə tətbiqi üçün <DİL ADI> (<dil kodu>) tərcüməçisisən.
Tətbiq Kotlin proqramlaşdırma dilini sıfırdan öyrədir; mənbə dil
azərbaycancadır.

Aşağıdakı POLICY və GLOSSARY qaydalarına tam əməl et.

Sənə JSON parçası verilir. Onun dəyərləri azərbaycancadır.
- YALNIZ dəyərləri tərcümə et; açarlara, onların sırasına və JSON
  quruluşuna toxunma.
- Kod olan dəyərlərdə (starter, hell, code) yalnız `//` və `/* */`
  şərhlərinin içini tərcümə et. Kodun qalanını — boşluqlar, sətir
  keçidləri və dırnaq içindəki mətn daxil — simvolu-simvoluna köçür.
- Span massivlərində ({"k": …, "v": …}) yalnız "v"-ni tərcümə et;
  "k" və "href" dəyişmir. `"k":"code"` span-ının "v"-si də dəyişmir.
- `opts` massivində elementlərin sayını və sırasını dəyişmə.
- Mətndə dırnaq içində sitat gətirilən kod literalını ("Salam, Kotlin!")
  tərcümə etmə — kodda o dəyişmir.
- Lüğətdəki termin üçün YALNIZ lüğətdəki qarşılığı işlət.
  doNotTranslate siyahısındakı adları olduğu kimi saxla.
- %1$s, %2$d, {0} nişanlarını saxla.
- Müraciət qeyri-rəsmi «sən» formasındadır; mətni qısaltma, genişləndirmə.

Cavab olaraq YALNIZ tərcümə olunmuş JSON-u qaytar — eyni açarlarla,
izahsız, ``` çərçivəsiz.

=== POLICY ===
<localization/POLICY.md-nin tam mətni>

=== GLOSSARY ===
<terms: hər termin üçün az → hədəf dil; doNotTranslate siyahısı>

=== JSON ===
<parça>
````

---

## Sınaq — en, 2026-11-12

Şablon `sintaksis` mövzusunun 3 nəzəri (`n-01…03`) və 3 praktiki
(`p-01…03`) çalışması üzərində yoxlandı — 17 sahə. Tərcümə en overlay-ə
yazıldı, yoxlandı, sonra geri alındı: T4.1 dalğası təmiz başlayır.

**Düzgün tərcümə:**

| Yoxlama | Nəticə |
|---|---|
| `validate-locales.js en` — xəta | 0 |
| `build-locale.js en` | 17 sahə tətbiq olundu, rədd 0 |
| `starter`-də yalnız şərh dəyişdi (`// Kodu bura yaz` → `// Write your code here`) | skelet eynidir, qəbul olundu |
| tapşırıqdakı `"Salam, Kotlin!"`, `"Aysel"` sitatları | saxlanıldı |
| xəbərdarlıq | 1 — `n-01 › opts` «bazadakı mətnin eynisi» (bax aşağı) |

**Qəsdən pozulmuş tərcümə** (validatorun tutduğunu yoxlamaq üçün):

| Pozuntu | Nəticə |
|---|---|
| `starter`-də `val ad` → `val name` | **xəta**: «sətir 2: kod dəyişib»; `build-locale` bazanı saxladı |
| `exp`-dən «Kotlin» silindi, «Java» → «Ява» | 2 **xəbərdarlıq**: «tərcümə olunmamalıdır» |

Qeydlər:
- Variantları tamamilə kod olan sualda (`start()`, `main()`…) `opts`
  tərcümədə dəyişmir — «bazadakı mətnin eynisi» xəbərdarlığı burada
  gözləniləndir, düzəliş tələb etmir.
- Bu parçada lüğət termini yox idi; lüğət yoxlamasının özü
  `validate-locales.js`-in `lugetYoxla` funksiyasındadır və T4.1-in ilk
  dərs bölmələrində (null safety, lambda) işə düşəcək.
