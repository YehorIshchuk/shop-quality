---
title: Локальне середовище
owner: Yehor Ishchuk
status: published
review_date: 2027-02-19
tags: [developer, ops]
---

# Локальне середовище розробника

Мета — за 15 хвилин мати зібраний проєкт, працююче API і локальний сайт документації.
Середовища, порти, змінні оточення і RTO/RPO описано в [[isd]] (ISD) — тут лише кроки для робочої станції.

## 1. Інструменти

| Що | Версія | Перевірка |
|---|---|---|
| JDK | 17+ (для SonarQube — окремо 21) | `java -version` |
| Maven | 3.9+ | `mvn -v` |
| Python | 3.9+ | `python3 --version` |
| Git | будь-яка сучасна | `git --version` |
| Obsidian (необов'язково) | будь-яка | відкрити корінь репозиторію як Vault |

## 2. Код: збірка, тести, запуск

```bash
mvn -B clean verify                                    # збірка + тести + покриття JaCoCo
java -cp target/classes com.kira.shop.ShopApplication  # демо + HTTP API на :8080 (Ctrl+C — зупинка)
```

## 3. Документація: локальний сайт MkDocs

```bash
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements-docs.txt
mkdocs serve                    # http://127.0.0.1:8000 — перезбирається при збереженні файлу
mkdocs build --strict           # те саме, що в CI: будь-яке попередження = помилка
```

## 4. Конвеєр якості коду (SonarQube, OO-гейти, doc-гейт)

```bash
# SonarQube 26.x із ZIP на JDK 21 (подробиці й пастки встановлення — ISD, вузол D4)
export SONAR_JAVA_PATH=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java
~/sonarqube-26.x/bin/macosx-universal-64/sonar.sh start      # http://localhost:9000
export SONAR_TOKEN=squ_...                 # My Account -> Security -> Generate Token (не комітити!)
./scripts/setup-quality-gate.sh            # кастомний Quality Gate "Shop-Quality-Gate"
./scripts/scan.sh                          # build + tests + JaCoCo + SonarQube

pip install -r tools/requirements.txt
./scripts/oo-audit.sh current              # CK / MOOD / Coupling Matrix -> reports/oo/current/
python3 tools/doc_audit.py --gate          # актуальність документації (ЛР10)
```

Альтернатива для Linux/Windows із Docker: `docker compose up -d`
([docker-compose.yml](https://github.com/YehorIshchuk/shop-quality/blob/main/docker-compose.yml)). Windows (PowerShell): ті самі скрипти з розширенням `.ps1`.

## 5. Obsidian як редактор бази знань

Відкрити **корінь репозиторію** як Vault (*Open folder as vault*). Зв'язки між документами — wikilinks
(ім'я файлу в подвійних квадратних дужках, див. блоки «Пов'язані документи»), теги — поле `tags` у front matter (`#architecture`, `#api`, `#ops`, `#developer`).
Папка `.obsidian/` у `.gitignore`. Граф знань: *Open graph view* (Cmd+G).

## Пов'язані документи

[[CONTRIBUTING]] · [[coding-standards]] · [[isd]] · [[handover]]
