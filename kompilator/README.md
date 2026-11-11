# ⛏️ Kompilator.pl

Strona, na którą wrzucasz źródła moda Minecraft w `.zip`, a serwer kompiluje je i oddaje gotowy plik `.jar`.

## ✦ Funkcje
- Drag & drop pliku `.zip`, podgląd logu Gradle **na żywo** (SSE), pobranie `.jar` i logu
- Automatyczne wykrywanie: **Fabric, Forge, NeoForge, Quilt, Paper/Spigot, Velocity**, Gradle (z `gradlew` lub bez) i **Maven**
- Automatyczny dobór Javy (8 / 17 / 21) z `build.gradle` / `gradle.properties` (`minecraft_version`), albo ręczny wybór
- Wybór właściwego jara (pomija `-sources`, `-dev`, `-javadoc`), obsługa projektów wielomodułowych (Architectury)
- Kolejka zadań, limit czasu kompilacji, limit rozmiaru, ochrona przed zip-slip i zip-bombami
- Automatyczne kasowanie plików po 30 min

## ✦ Uruchomienie
```bash
cd kompilator
npm install
npm start            # http://localhost:3000
```

### Tryb lokalny (domyślny) – tylko do testów u siebie
Używa `gradle`/`mvn`/`java` zainstalowanych na komputerze. Różne JDK wskażesz zmiennymi:
`JAVA_8_HOME`, `JAVA_17_HOME`, `JAVA_21_HOME`.

### Tryb Docker – **obowiązkowy, jeśli strona ma być publiczna**
```bash
BUILD_MODE=docker npm start
```
Każdy build idzie w osobnym kontenerze (`gradle:8.14-jdk{8,17,21}` / `maven:3.9-eclipse-temurin-*`)
z limitem RAM/CPU, bez uprawnień (`--cap-drop=ALL`). Cache Gradle i Maven trzymany jest w wolumenach Dockera,
więc kolejne buildy są szybsze.

> ⚠️ Plik `build.gradle` to kod, który wykonuje się podczas kompilacji. Na publicznym serwerze **nigdy** nie
> uruchamiaj kompilatora w trybie lokalnym – ktoś mógłby przejąć maszynę. Najlepiej postaw go na osobnym VPS.

## ✦ Konfiguracja (zmienne środowiskowe)
| Zmienna | Domyślnie | Opis |
|---|---|---|
| `PORT` | `3000` | port strony |
| `BUILD_MODE` | `local` | `local` albo `docker` |
| `CONCURRENCY` | `1` | ile buildów naraz |
| `BUILD_TIMEOUT_MIN` | `15` | limit czasu jednego builda |
| `MAX_UPLOAD_MB` | `50` | max rozmiar `.zip` |
| `MAX_UNZIPPED_MB` | `300` | max rozmiar po rozpakowaniu |
| `KEEP_MIN` | `30` | po ilu minutach kasować pliki |
| `DOCKER_MEMORY` / `DOCKER_CPUS` | `3g` / `2` | limity kontenera |
| `WORK_DIR` | `/tmp/kompilator` | katalog roboczy |

## ✦ API
- `POST /api/compile` – multipart: `mod` (plik .zip), `java` (`auto`/`8`/`17`/`21`) → `{ id, status }`
- `GET /api/jobs/:id` – status zadania
- `GET /api/jobs/:id/events` – log na żywo (Server-Sent Events)
- `GET /api/jobs/:id/download` – gotowy `.jar`
- `GET /api/jobs/:id/log` – pełny log jako `.txt`
