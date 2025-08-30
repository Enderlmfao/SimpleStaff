package com.ender.staffMode

import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

data class PlayerState(
    val inventoryContents: Array<ItemStack?>,
    val armorContents: Array<ItemStack?>,
    val location: Location,
    val gameMode: GameMode,
    val allowFlight: Boolean,
    val health: Double,
    val foodLevel: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlayerState

        if (allowFlight != other.allowFlight) return false
        if (health != other.health) return false
        if (foodLevel != other.foodLevel) return false
        if (!inventoryContents.contentEquals(other.inventoryContents)) return false
        if (!armorContents.contentEquals(other.armorContents)) return false
        if (location != other.location) return false
        if (gameMode != other.gameMode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = allowFlight.hashCode()
        result = 31 * result + health.hashCode()
        result = 31 * result + foodLevel
        result = 31 * result + inventoryContents.contentHashCode()
        result = 31 * result + armorContents.contentHashCode()
        result = 31 * result + location.hashCode()
        result = 31 * result + gameMode.hashCode()
        return result
    }
}

class PlayerStateManager {
    private val savedStates = mutableMapOf<UUID, PlayerState>()

    fun savePlayerState(player: Player) {
        val state = PlayerState(
            inventoryContents = player.inventory.contents.clone(),
            armorContents = player.inventory.armorContents.clone(),
            location = player.location,
            gameMode = player.gameMode,
            allowFlight = player.allowFlight,
            health = player.health,
            foodLevel = player.foodLevel
        )
        savedStates[player.uniqueId] = state
    }

    fun restorePlayerState(player: Player) {
        savedStates.remove(player.uniqueId)?.let { state ->
            player.inventory.contents = state.inventoryContents
            player.inventory.armorContents = state.armorContents
            player.teleport(state.location)
            player.gameMode = state.gameMode
            player.allowFlight = state.allowFlight
            player.health = state.health
            player.foodLevel = state.foodLevel
        }
    }
}