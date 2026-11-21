# Лабораторна робота №1. Автоматизований контроль якості програмного проєкту

> 🗄️ **Історичний документ процесу (ЛР1, 2026-11-15)** — фіксує стан системи на момент виконання і не оновлюється.
> Актуальна архітектура — [SDD](sdd.md), розгортання — [ISD](isd.md).

> Поля `⟨…⟩` заповнюються з SonarQube після сканування станів за інструкцією `docs/DEMO.md` §2.
> Значення Cognitive/Cyclomatic Complexity нижче пораховано інструментом `tools/oo_metrics.py`
> за методикою SonarSource — звірте їх із вкладкою *Measures → Complexity* у SonarQube.

## 1. Об'єкт дослідження

- **Проєкт:** *Shop Quality* — ядро інтернет-магазину: каталог, склад, замовлення, оплата, знижки, доставка, звіти.
- **Стек:** Java 17, Maven, JUnit 5, JaCoCo.
- **Обсяг:** ≈ 1 300 рядків основного коду (22 класи) + ≈ 400 рядків тестів (18–22 тести залежно від стану).
- **SAST:** SonarQube Community Edition (Docker, `docker-compose.yml`, порт 9000), аналіз через `sonar-maven-plugin`
  (альтернатива — CLI `sonar-scanner` з `sonar-project.properties`).
- **Автоматизація:** `scripts/scan.sh` / `scan.ps1` виконує `mvn clean verify sonar:sonar` з
  `sonar.qualitygate.wait=true` — збірка **завершується з помилкою, якщо Quality Gate = FAILED**.
  Quality Gate створюється скриптом `scripts/setup-quality-gate.sh|.ps1` через Web API.

## 2. Первинний аналіз (Baseline Scan) — тег `lab1-baseline`

Скріншот: ⟨Overview після першого сканування⟩

| Метрика | Значення |
|---|---|
| Bugs (Blocker / Critical / Major) | ⟨…⟩ |
| Vulnerabilities | ⟨…⟩ |
| Security Hotspots | ⟨…⟩ |
| Code Smells | ⟨…⟩ |
| Technical Debt | ⟨…⟩ |
| Duplicated Lines % | ⟨…⟩ |
| Cyclomatic Complexity (проєкт) | ⟨…⟩ |
| Cognitive Complexity (проєкт) | ⟨…⟩ |
| Coverage % | ⟨…⟩ |
| Найгірший метод (Cognitive) | `StoreManager.placeOrder` — **49** (Cyclomatic 13) |
| Другий найгірший | `StoreManager.buildCustomerSummary` — **39** (Cyclomatic 13) |

Обидва методи перевищують поріг правила `java:S3776` (15) — Sonar позначає їх як *Critical Code Smell*.
Перевірка оновлення метрик: коміт `docs: clarify InventoryStore javadoc …` — повторний аналіз оновлює
кількість рядків коментарів/`comment_lines_density` при незмінних інших метриках.

## 3. Quality Gate «Shop-Quality-Gate»

Скріншот: ⟨Quality Gates → Shop-Quality-Gate⟩

| # | Категорія | Умова | Поріг | Обґрунтування |
|---|---|---|---|---|
| 1 | New Code | New Bugs | `> 0` → FAIL | Магазин працює з грошима й залишками: будь-який новий дефект надійності (порівняння рядків через `==`, неточний `BigDecimal`) напряму спотворює суми чи стан складу. Нуль толерантності на новому коді не створює боргу для команди. *(На інстансах у режимі MQR, де немає `new_bugs`, скрипт ставить еквівалент `New Reliability Rating > A`.)* |
| 2 | New Code | Security Hotspots Reviewed | `< 100 %` → FAIL | Сервіс обробляє платежі, купони й персональні дані. Кожне потенційно небезпечне місце (PRNG, крипто, файлові шляхи) має бути переглянуте людиною до злиття. |
| 3 | Overall | Duplicated Lines % | `> 3 %` → FAIL | Baseline ≈ 0 %. Дублювання бізнес-правил (знижки, рекомендації рівня) — головне джерело розбіжностей, коли правило змінюють в одному місці й забувають в іншому. 3 % — жорсткіше за ліміт методички (5 %), бо кодова база мала і кожен дубль помітний. |
| 4 | Overall | Maintainability Rating | гірше за `A` → FAIL | Rating A = технічний борг ≤ 5 % від оціненої вартості розробки. Не дає проєкту деградувати нижче критичної межі, але допускає локальні «гарячі точки» (S3776), які системно усуваються в Завд.4. |

Підтвердження: ⟨скріншот статусу PASS для baseline⟩

## 4. Краш-тест (тег `lab1-fail`)

Коміт `test(quality): inject intentional violations to trigger Quality Gate, FAIL`:

1. **Дублювання (Overall, умова 3).** `buildCustomerSummary` скопійовано у `buildCustomerSummaryForMonth`
   (≈ 40 рядків, відрізняється однією умовою фільтрації).
2. **Нові Bugs (New Code, умова 1).**
   ```java
   if (code == "WELCOME10") {          // java:S4973 — порівняння посилань, а не вмісту рядків
       rate = new BigDecimal(0.1);     // java:S2111 — 0.1000000000000000055511151231257827…
   }
   ```
   Тест при цьому проходить (літерали інтерновані) — тобто дефект прихований і виявляється лише SAST.
3. **Security Hotspot (New Code, умова 2).** `new Random()` для генерації кодів купонів (`java:S2245`) —
   передбачуваний PRNG.

Скріншот: ⟨Overview зі статусом FAILED і списком порушених умов⟩

## 5. Відновлення та рефакторинг

**Тег `lab1-pass` — виправлення (не видалення!):**
- дубль винесено у багаторазовий метод `summarize(Customer, Predicate<Order> filter, String label)`;
  обидва публічні методи стали однорядковими делегатами;
- купони — таблиця `Map<String, BigDecimal> COUPON_RATES` (немає порівняння рядків взагалі),
  `new BigDecimal("0.10")` — точне значення;
- `Random` → `SecureRandom` (hotspot зникає).

**Тег `lab1-refactor` — Завд.4, зниження Cognitive Complexity:**

| Метод | До (Cog / Cyc) | Після (Cog / Cyc) | Техніки |
|---|---|---|---|
| `placeOrder` | 49 / 13 | 2 / 3 | Guard Clauses (`requireArgument`), Extract Method: `findProduct`, `resolveQuantity`, `reserveStock`, `isPremium` |
| `summarize` (був `buildCustomerSummary`) | 40 / 14 | 0 / 1 | Заміна вкладених циклів/умов stream-конвеєром; Extract Method: `isCompleted`, `isPremiumPurchase`, `recommendTier`; іменовані константи замість magic numbers |

Найгірші з винесених методів — `resolveQuantity` / `recommendTier`, Cognitive **5** (≤ 15); найгірший метод проєкту теж 5. Методів > 15: **2 → 0**.

«До» (фрагмент):
```java
if (customer != null) {
    if (cart != null && !cart.isEmpty()) {
        ...
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            Optional<Product> product = productRepository.findById(entry.getKey());
            if (product.isPresent()) {
                if (entry.getValue() != null && entry.getValue() > 0) {
                    if (inventory.getStock(product.get().getId()) >= entry.getValue()) {
                        order.addItem(product.get(), entry.getValue());
                    } else {
                        if (customer.getTier() == CustomerTier.GOLD || ...) {   // вкладеність 6
```
«Після»:
```java
requireArgument(customer != null, "Customer is required");
requireArgument(cart != null && !cart.isEmpty(), "Cart is empty");
Order order = new Order("ORD-" + (orderSequence++), customer, LocalDate.now());
for (Map.Entry<String, Integer> entry : cart.entrySet()) {
    Product product = findProduct(entry.getKey());
    order.addItem(product, resolveQuantity(customer, product, entry.getValue()));
}
reserveStock(order);
```
Повний diff: `git diff lab1-pass lab1-refactor`.

**Технічний борг.** SonarQube рахує борг за SQALE: кожне правило має ремедіаційну функцію;
для `java:S3776` це *5 хв базово + 1 хв за кожну одиницю понад поріг 15*: `placeOrder` → 5 + 34 = 39 хв,
`buildCustomerSummary` → 5 + 24 = 29 хв (звірте з *Issues → Effort*). Після рефакторингу ці issues закриваються.
Борг до/після: ⟨…⟩ → ⟨…⟩.

## 6. Порівняльна таблиця

| Показник | Baseline (`lab1-baseline`) | Failed (`lab1-fail`) | Fixed (`lab1-pass` → `lab1-refactor`) |
|---|---|---|---|
| Bugs | ⟨0⟩ | ⟨2⟩ | ⟨0⟩ |
| Vulnerabilities / Hotspots | ⟨0 / 0⟩ | ⟨0 / 1⟩ | ⟨0 / 0⟩ |
| Code Smells | ⟨…⟩ | ⟨…⟩ | ⟨…⟩ |
| Technical Debt | ⟨…⟩ | ⟨…⟩ | ⟨…⟩ |
| Duplicated Lines % | ⟨≈0⟩ | ⟨≈6⟩ | ⟨≈0⟩ |
| Cognitive Complexity (найгірший метод) | 49 | 49 | 49 → **5** |
| Quality Gate | PASS | FAIL | PASS |

## 7. Висновок

Cyclomatic Complexity оцінює кількість шляхів виконання (а отже — мінімум тестів), але однаково
рахує плаский `switch` на 10 гілок і 10 вкладених `if`. Cognitive Complexity штрафує саме вкладеність
і переривання лінійного читання, тому краще відображає зусилля на розуміння: `placeOrder` мав
Cyclomatic 13 (помірно), але Cognitive 49 (критично); після Guard Clauses та Extract Method —
2 у самому методі (найгірший з винесених — 5) при майже незмінній сумарній цикломатичній складності
класу (WMC 68 → 67 — логіка не зникла, а розподілилась по іменованих методах).

Автоматичний контроль технічного боргу через Quality Gate доцільний у комерційних проєктах:
блокування збірки на новому коді (Clean as You Code) запобігає накопиченню боргу без вимоги
одразу виправити весь legacy, а пороги на загальному коді фіксують «нижню межу» якості.
Експеримент показав, що прихований дефект (`==` для рядків) проходить юніт-тести, але
зупиняється SAST-гейтом ще до злиття.
