package com.ender.staffMode

import org.bukkit.ChatColor
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class StaffModeCommand(
    private val plugin: StaffMode,
    private val playerStateManager: PlayerStateManager
) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command can only be used by players.")
            return true
        }
        if (!sender.hasPermission("staffmode.use")) {
            sender.sendMessage("${ChatColor.RED}You do not have permission to use this command.")
            return true
        }
        if (plugin.staffModePlayers.contains(sender.uniqueId)) {
            disableStaffMode(sender)
        } else {
            enableStaffMode(sender)
        }
        return true
    }

    private fun enableStaffMode(player: Player) {
        plugin.staffModePlayers.add(player.uniqueId)
        playerStateManager.savePlayerState(player)
        plugin.server.onlinePlayers.forEach { otherPlayer ->
            if (!otherPlayer.hasPermission("staffmode.use")) {
                otherPlayer.hidePlayer(plugin, player)
            }
        }
        player.gameMode = GameMode.CREATIVE
        player.inventory.clear()

        player.inventory.setItem(1, createNavigatorTool())
        player.inventory.setItem(3, createFreezeTool())
        player.inventory.setItem(5, createInspectorTool())
        player.inventory.setItem(7, createInvseeTool())

        player.sendMessage("${ChatColor.GREEN}Staff mode enabled. You are now invisible to regular players.")
    }

    private fun disableStaffMode(player: Player) {
        plugin.staffModePlayers.remove(player.uniqueId)
        player.inventory.clear()
        playerStateManager.restorePlayerState(player)
        plugin.server.onlinePlayers.forEach { otherPlayer ->
            otherPlayer.showPlayer(plugin, player)
        }
        player.sendMessage("${ChatColor.RED}Staff mode disabled. You are now visible.")
    }

    private fun createNavigatorTool(): ItemStack {
        val navigator = ItemStack(Material.COMPASS)
        val meta = navigator.itemMeta
        meta?.setDisplayName("${ChatColor.GREEN}${ChatColor.BOLD}Player Navigator")
        meta?.lore = listOf(
            "${ChatColor.GRAY}Right-Click: Open player list.",
            "${ChatColor.GRAY}Left-Click: Teleport to block."
        )
        navigator.itemMeta = meta
        return navigator
    }

    private fun createInspectorTool(): ItemStack {
        val inspector = ItemStack(Material.SPYGLASS)
        val meta = inspector.itemMeta
        meta?.setDisplayName("${ChatColor.YELLOW}${ChatColor.BOLD}Player Inspector")
        meta?.lore = listOf("${ChatColor.GRAY}Right-click a player for details.")
        inspector.itemMeta = meta
        return inspector
    }

    private fun createInvseeTool(): ItemStack {
        val invseeBook = ItemStack(Material.BOOK)
        val meta = invseeBook.itemMeta
        meta?.setDisplayName("${ChatColor.AQUA}${ChatColor.BOLD}Invsee")
        meta?.lore = listOf("${ChatColor.GRAY}Right-click a player to view their inventory.")
        invseeBook.itemMeta = meta
        return invseeBook
    }

    private fun createFreezeTool(): ItemStack {
        val freezeIce = ItemStack(Material.ICE)
        val meta = freezeIce.itemMeta
        meta?.setDisplayName("${ChatColor.AQUA}${ChatColor.BOLD}Freeze")
        meta?.lore = listOf("${ChatColor.GRAY}Right-click a player to freeze them.")
        freezeIce.itemMeta = meta
        return freezeIce
    }
}