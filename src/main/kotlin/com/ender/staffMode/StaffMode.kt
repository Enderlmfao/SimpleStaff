package com.ender.staffMode

import com.ender.staffMode.PlayerStateManager
import com.ender.staffMode.StaffModeCommand
import com.ender.staffMode.StaffModeListener
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

class StaffMode : JavaPlugin() {

    val staffModePlayers = mutableSetOf<UUID>()
    val frozenPlayers = mutableSetOf<UUID>()
    val frozenTitleTasks = mutableMapOf<UUID, BukkitTask>()

    private val playerStateManager = PlayerStateManager()

    override fun onEnable() {
        getCommand("h")?.setExecutor(StaffModeCommand(this, playerStateManager))
        server.pluginManager.registerEvents(StaffModeListener(this, playerStateManager), this)
        logger.info("StaffMode has been enabled.")
    }

    override fun onDisable() {
        frozenTitleTasks.values.forEach { it.cancel() } // Clean up tasks on shutdown
        server.onlinePlayers.filter { staffModePlayers.contains(it.uniqueId) }.forEach { player ->
            playerStateManager.restorePlayerState(player)
            logger.info("Restored ${player.name} from staff mode due to plugin shutdown.")
        }
        logger.info("StaffMode has been disabled.")
    }
}