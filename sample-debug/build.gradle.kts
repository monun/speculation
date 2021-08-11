repositories {
    mavenLocal()
}

val projectAPI = project(":${rootProject.name}-api")
val projectCore = project(":${rootProject.name}-core")

dependencies {
    implementation(projectAPI)
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

    create<Jar>("debugMojangJar") {
        archiveBaseName.set(pluginName)
        archiveVersion.set("")
        archiveClassifier.set("MOJANG")

        (listOf(projectAPI, project, projectCore) + projectCore.subprojects).forEach {
            from(it.sourceSets["main"].output)
        }

        from(project.sourceSets["main"].output)
        exclude("paper-plugin.yml")
        rename("mojang-plugin.yml", "plugin.yml")

        doLast {
            from(archiveFile)
            val plugins = File(rootDir, ".debug-paper/plugins/")
            into(if (File(plugins, archiveFileName.get()).exists()) File(plugins, "update") else plugins)
        }
    }

    create<Jar>("debugPaperJar") {
        dependsOn(projectAPI.tasks.named("publishAllPublicationsToDebugRepository"))
        dependsOn(projectCore.tasks.named("publishAllPublicationsToDebugRepository"))

        archiveBaseName.set(pluginName)
        archiveVersion.set("")
        archiveClassifier.set("PAPER")

        from(project.sourceSets["main"].output)
        exclude("mojang-plugin.yml")
        rename("paper-plugin.yml", "plugin.yml")

        doLast {
            copy {
                from(archiveFile)
                val plugins = File(rootDir, ".debug-paper/plugins/")
                into(if (File(plugins, archiveFileName.get()).exists()) File(plugins, "update") else plugins)
            }
        }
    }
}
