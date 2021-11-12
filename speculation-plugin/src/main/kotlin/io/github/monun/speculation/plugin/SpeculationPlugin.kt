package io.github.monun.speculation.plugin

import org.bukkit.plugin.java.JavaPlugin

class SpeculationPlugin: JavaPlugin() {
    override fun onEnable() {
        logger.info("HELLO WORLD!")
    }
}