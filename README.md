# Paper multi project (1.17+)

[![Kotlin](https://img.shields.io/badge/java-16.0.2-ED8B00.svg?logo=java)](https://www.azul.com/)
[![Kotlin](https://img.shields.io/badge/kotlin-1.5.30-585DEF.svg?logo=kotlin)](http://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/gradle-7.2-02303A.svg?logo=gradle)](https://gradle.org)
[![GitHub](https://img.shields.io/github/license/monun/paper-sample-multi)](https://www.gnu.org/licenses/gpl-3.0.html)
[![Kotlin](https://img.shields.io/badge/youtube-각별-red.svg?logo=youtube)](https://www.youtube.com/channel/UCDrAR1OWC2MD4s0JLetN0MA)

---

### 프로젝트 구성

1. 프로젝트 이름 변경
    * `settings.gradle.kts / rootProject.name=sample`
2. 서브프로젝트 구성
    * `./gradlew setupModules`

---

### 의존성 가져오기 `net.minecraft.server`

1. core 프로젝트 하위에 버전 이름의 프로젝트 생성
    * `:core:v1.17`
    * `:core:v1.17.1`
2. 태스크 실행
    * `./gradlew setupDependencies`

---
* `./gradlew setupWorkspace` = 모든 setup 태스크 실행