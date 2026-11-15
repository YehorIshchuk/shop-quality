# Shop Quality

Ядро інтернет-магазину (Java 17, Maven): каталог, склад, замовлення, оплата, знижки, доставка, звіти.
Проєкт використовується для лабораторних робіт з автоматизованого контролю якості (SonarQube).

## Швидкий старт

```bash
docker compose up -d                       # SonarQube на http://localhost:9000
export SONAR_TOKEN=squ_...                 # My Account -> Security -> Generate Token
./scripts/setup-quality-gate.sh            # кастомний Quality Gate
./scripts/scan.sh                          # build + tests + JaCoCo + SonarQube analysis
```

Windows (PowerShell): `.\scripts\setup-quality-gate.ps1`, `.\scripts\scan.ps1`.
