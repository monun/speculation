val core = project(":${rootProject.name}-core")

repositories {
    maven("https://papermc.io/repo/repository/maven-public/")
}

dependencies {
    implementation(core)
    implementation("io.papermc.paper:paper-api:1.17.1-R0.1-SNAPSHOT")
    implementation("io.github.monun:heartbeat-coroutines:0.0.1")
    implementation("io.github.monun:tap-api:4.1.10")
    implementation("io.github.monun:kommand-api:2.6.6")
}

val pluginName = rootProject.name.split('-').joinToString("") { it.capitalize() }
val packageName = rootProject.name.replace("-", "")
extra.set("pluginName", pluginName)
extra.set("packageName", packageName)

tasks {
    processResources {
        filesMatching("*.yml") {
            expand(project.properties)
            expand(extra.properties)
        }
    }

    create<Jar>("paperJar") {
        archiveBaseName.set(pluginName)
        archiveVersion.set("")

        (listOf(project, core) + core.subprojects).forEach {
            from(it.sourceSets["main"].output)
        }

        doLast {
            copy {
                from(archiveFile)
                val plugins = File(rootDir, ".debug/plugins/")
                into(if (File(plugins, archiveFileName.get()).exists()) File(plugins, "update") else plugins)
            }
        }
    }
}
