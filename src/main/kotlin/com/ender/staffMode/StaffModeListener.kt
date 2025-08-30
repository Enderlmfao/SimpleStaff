package com.ender.staffMode

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.scheduler.BukkitRunnable

class StaffModeListener(
    private val plugin: StaffMode,
    private val playerStateManager: PlayerStateManager
) : Listener {

    companion object {
        val NAVIGATOR_GUI_TITLE = "${ChatColor.DARK_BLUE}Player Navigator"
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val joinedPlayer = event.player
        plugin.server.onlinePlayers.forEach { onlinePlayer ->
            if (onlinePlayer.uniqueId == joinedPlayer.uniqueId) return@forEach
            if (plugin.staffModePlayers.contains(onlinePlayer.uniqueId)) {
                joinedPlayer.hidePlayer(plugin, onlinePlayer)
            }
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        if (plugin.staffModePlayers.contains(player.uniqueId)) {
            plugin.staffModePlayers.remove(player.uniqueId)
            playerStateManager.restorePlayerState(player)
        }
        if (plugin.frozenPlayers.contains(player.uniqueId)) {
            plugin.frozenTitleTasks[player.uniqueId]?.cancel()
            plugin.frozenTitleTasks.remove(player.uniqueId)
            plugin.frozenPlayers.remove(player.uniqueId)
            val command = "ban ${player.name} 7d [StaffMode] Logged out while frozen."
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
        }
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        if (damager is Player && plugin.staffModePlayers.contains(damager.uniqueId)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onEntityPickupItem(event: EntityPickupItemEvent) {
        val entity = event.entity
        if (entity is Player && plugin.staffModePlayers.contains(entity.uniqueId)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        if (plugin.staffModePlayers.contains(event.player.uniqueId)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (plugin.staffModePlayers.contains(event.player.uniqueId)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (plugin.staffModePlayers.contains(event.player.uniqueId)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!plugin.staffModePlayers.contains(player.uniqueId)) return

        val itemInHand = player.inventory.itemInMainHand
        if (itemInHand.type == Material.COMPASS && itemInHand.hasItemMeta() && itemInHand.itemMeta?.displayName?.contains("Navigator") == true) {
            event.isCancelled = true
            if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
                openNavigatorGui(player)
            } else if (event.action == Action.LEFT_CLICK_AIR || event.action == Action.LEFT_CLICK_BLOCK) {
                val targetBlock = player.getTargetBlockExact(100)
                if (targetBlock != null) {
                    player.teleport(targetBlock.location.add(0.0, 1.0, 0.0))
                }
            }
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        if (event.view.title == NAVIGATOR_GUI_TITLE) {
            event.isCancelled = true
            val clickedItem = event.currentItem ?: return
            if (clickedItem.type == Material.PLAYER_HEAD) {
                val targetName = ChatColor.stripColor(clickedItem.itemMeta?.displayName)
                val targetPlayer = Bukkit.getPlayer(targetName ?: "")
                if (targetPlayer != null) {
                    player.teleport(targetPlayer)
                    player.closeInventory()
                } else {
                    player.sendMessage("${ChatColor.RED}Player not found.")
                    player.closeInventory()
                }
            }
            return
        }

        if (plugin.staffModePlayers.contains(player.uniqueId) && player.gameMode == GameMode.CREATIVE) {
            if (event.clickedInventory != player.inventory) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onPlayerInteractAtEntity(event: PlayerInteractAtEntityEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        val staff = event.player
        if (!plugin.staffModePlayers.contains(staff.uniqueId)) return
        val target = event.rightClicked as? Player ?: return
        val itemInHand = staff.inventory.itemInMainHand

        when {
            itemInHand.type == Material.SPYGLASS && itemInHand.itemMeta?.displayName?.contains("Inspector") == true -> {
                event.isCancelled = true
                sendPlayerReport(staff, target)
            }
            itemInHand.type == Material.BOOK && itemInHand.itemMeta?.displayName?.contains("Invsee") == true -> {
                event.isCancelled = true
                staff.openInventory(target.inventory)
            }
            itemInHand.type == Material.ICE && itemInHand.itemMeta?.displayName?.contains("Freeze") == true -> {
                event.isCancelled = true
                toggleFreeze(staff, target)
            }
        }
    }

    private fun openNavigatorGui(player: Player) {
        val onlinePlayers = Bukkit.getOnlinePlayers().filter { it.uniqueId != player.uniqueId }
        val guiSize = (onlinePlayers.size / 9 + 1) * 9
        val gui = Bukkit.createInventory(null, guiSize, NAVIGATOR_GUI_TITLE)

        onlinePlayers.forEach { target ->
            val head = ItemStack(Material.PLAYER_HEAD)
            val meta = head.itemMeta as SkullMeta
            meta.setOwningPlayer(target)
            meta.setDisplayName("${ChatColor.YELLOW}${target.name}")
            head.itemMeta = meta
            gui.addItem(head)
        }
        player.openInventory(gui)
    }

    private fun sendPlayerReport(staff: Player, target: Player) {
        staff.sendMessage("${ChatColor.GRAY}--- ${ChatColor.YELLOW}Report: ${target.name} ${ChatColor.GRAY}---")
        staff.sendMessage("${ChatColor.GOLD}Gamemode: ${ChatColor.WHITE}${target.gameMode}")
        staff.sendMessage("${ChatColor.GOLD}Health: ${ChatColor.WHITE}${String.format("%.1f", target.health)} / 20")
        staff.sendMessage("${ChatColor.GOLD}Location: ${ChatColor.WHITE}${target.location.world?.name} (${target.location.blockX}, ${target.location.blockY}, ${target.location.blockZ})")
        staff.sendMessage("${ChatColor.GOLD}IP Address: ${ChatColor.WHITE}${target.address?.hostString ?: "N/A"}")
    }

    private fun toggleFreeze(staff: Player, target: Player) {
        if (plugin.frozenPlayers.contains(target.uniqueId)) {
            plugin.frozenPlayers.remove(target.uniqueId)
            plugin.frozenTitleTasks[target.uniqueId]?.cancel()
            plugin.frozenTitleTasks.remove(target.uniqueId)
            staff.sendMessage("${ChatColor.YELLOW}You have unfrozen ${target.name}.")
            target.sendMessage("${ChatColor.GREEN}You have been unfrozen.")
            target.resetTitle()
        } else {
            plugin.frozenPlayers.add(target.uniqueId)
            staff.sendMessage("${ChatColor.AQUA}You have frozen ${target.name}.")
            val task = object : BukkitRunnable() {
                override fun run() {
                    if (!target.isOnline || !plugin.frozenPlayers.contains(target.uniqueId)) {
                        this.cancel()
                        return
                    }
                    target.sendTitle(
                        "${ChatColor.AQUA}${ChatColor.BOLD}YOU ARE FROZEN",
                        "${ChatColor.BLUE}Logging out will result in a 7-day ban.",
                        0, 40, 20
                    )
                }
            }.runTaskTimer(plugin, 0L, 30L)
            plugin.frozenTitleTasks[target.uniqueId] = task
        }
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (plugin.frozenPlayers.contains(event.player.uniqueId)) {
            if (event.from.x != event.to.x || event.from.y != event.to.y || event.from.z != event.to.z) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        if (plugin.frozenPlayers.contains(event.player.uniqueId)) {
            event.isCancelled = true
            event.player.sendMessage("${ChatColor.RED}You cannot speak while frozen.")
        }
    }
}