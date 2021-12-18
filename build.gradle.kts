import org.jetbrains.kotlin.gradle.targets.js.npm.includedRange

plugins {
    kotlin("jvm") version "1.5.21"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(16))
    }
}

allprojects {
    repositories {
        mavenCentral()
    }

    tasks {
        withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions {
                jvmTarget = "16"
            }
        }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        implementation(kotlin("stdlib"))
        implementation(kotlin("reflect"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")

        testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
        testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.2")
        testImplementation("org.mockito:mockito-core:3.6.28")
    }

    tasks {
        test {
            useJUnitPlatform()
        }
    }
}

tasks {
    val resourcepacksFolder = file("resourcepacks")
    val resourcepacksTasks = resourcepacksFolder.listFiles { file -> file.isDirectory }?.map { folder ->
        create<Zip>("resourcepacks-${folder.name}") {
            from(folder)
            include("*")
            include("*/**")
            archiveFileName.set("${folder.name}.zip")
            destinationDirectory.set(file("build/libs/"))
        }
    }

    create("resourcepacks") {
        dependsOn(resourcepacksTasks)
    }
}