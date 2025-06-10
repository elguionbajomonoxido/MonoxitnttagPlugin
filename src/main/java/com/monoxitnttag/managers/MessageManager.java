package com.monoxitnttag.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages all messages for the MonoxiTntTag plugin.
 * Centralizes message handling for game states, player events, and debugging.
 */
public class MessageManager {

    private final JavaPlugin plugin;
    private final Logger logger;
    private boolean debugMode = false;

    // Message prefixes with color codes using '&'
    public static final String PREFIX = "&7&l[&4&lTNT&f&ltTAG&7&l] &f";
    private static final String DEBUG_PREFIX = "&8[DEBUG] &7";
    private static final String ERROR_PREFIX = "&c[ERROR] &f";
    private static final String WARNING_PREFIX = "&e[ADVERTENCIA] &f";
    private static final String INFO_PREFIX = "&b[INFO] &f";

    /**
     * Get a message with the plugin prefix
     * @param message The message to prefix
     * @return The prefixed message
     */
    public String prefixed(String message) {
        return getColoredMessage(PREFIX + message);
    }

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * Enable or disable debug mode
     * @param enabled true to enable debug mode, false to disable
     */
    public void setDebugMode(boolean enabled) {
        this.debugMode = enabled;
        log(prefixed("Modo debug " + (enabled ? "activado" : "desactivado")));
    }

    /**
     * Check if debug mode is enabled
     * @return true if debug mode is enabled, false otherwise
     */
    public boolean isDebugMode() {
        return debugMode;
    }

    // ===== GAME START MESSAGES =====

    /**
     * Broadcast a welcome message to all players when the game starts
     * @param timeSeconds Duration of the game in seconds
     * @param playersWithTnt Number of players with TNT
     * @param playersWithoutTnt Number of players without TNT
     */
    public void broadcastGameStart(int timeSeconds, int playersWithTnt, int playersWithoutTnt) {
        String formattedTime = formatTime(timeSeconds);

        // General game start message - sent to all players
        broadcast(prefixed("&c¡El minijuego 'TNT Tag' ha comenzado!"));
        broadcast(prefixed("&eDuración: &f" + formattedTime));

        // Statistics for non-OP players only
        broadcast(prefixed("&eJugadores con TNT: &c" + playersWithTnt));
        broadcast(prefixed("&eJugadores sin TNT: &a" + playersWithoutTnt));
        broadcast(prefixed("&eObjetivo: &f¡Pasa la TNT a otros jugadores antes de que explote!"));

        debug("Juego iniciado con " + playersWithTnt + " jugadores con TNT y " +
                playersWithoutTnt + " jugadores sin TNT. Duración: " + timeSeconds + " segundos.");
    }

    /**
     * Send a personalized message to a player when they receive ears
     * @param player The player who received ears
     */
    public void sendEarsReceived(Player player) {
        player.sendMessage(prefixed("&c¡Has recibido la TNT activa!"));
        player.sendMessage(prefixed("&ePásala a otros jugadores antes de que explote."));

        debug("Jugador " + player.getName() + " ha recibido la TNT al inicio del juego.");
    }

    /**
     * Send a personalized message to a player when they don't receive ears
     * @param player The player who didn't receive ears
     */
    public void sendNoEarsWarning(Player player) {
        player.sendMessage(prefixed("&a¡No tienes la TNT activa!"));
        player.sendMessage(prefixed("&eEvita recibirla antes de que termine el juego o serás eliminado."));

        debug("Jugador " + player.getName() + " no tiene la TNT al inicio del juego.");
    }

    // ===== GAME PROGRESS MESSAGES =====

    /**
     * Broadcast a time update message
     * @param remainingSeconds Remaining time in seconds
     * @param totalSeconds Total game time in seconds
     */
    public void broadcastTimeUpdate(int remainingSeconds, int totalSeconds) {
        // Only broadcast at specific intervals to avoid spam
        if (remainingSeconds <= 10 ||
                remainingSeconds == 30 ||
                remainingSeconds == 60 ||
                remainingSeconds == 120 ||
                remainingSeconds == 180 ||
                remainingSeconds == 240 ||
                remainingSeconds % 300 == 0) { // Every 5 minutes

            String formattedTime = formatTime(remainingSeconds);
            broadcast(prefixed("&eTiempo restante: &f" + formattedTime));

            // Additional warning for last 10 seconds
            if (remainingSeconds <= 3) {
                broadcast(prefixed("&c¡El juego está a punto de terminar!"));
            }

            debug("Actualización de tiempo: " + remainingSeconds + "/" + totalSeconds +
                    " segundos restantes (" + (int)(((double)remainingSeconds/totalSeconds)*100) + "%)");
        }
    }

    /**
     * Send a message when a player steals ears from another player
     * @param thief The player who stole the ears
     * @param victim The player who lost the ears
     */
    public void broadcastEarsStolen(Player thief, Player victim) {
        broadcast(prefixed("&e" + thief.getName() + "&a ha robado las orejas de " + "&e" + victim.getName() + "&a!"));

        // Personal messages
        thief.sendMessage(prefixed("&a¡Has robado las orejas de " + victim.getName() + "!"));
        victim.sendMessage(prefixed("&c¡" + thief.getName() + " te ha robado las orejas!"));

        debug("Robo de orejas: " + thief.getName() + " robó orejas de " + victim.getName());
    }

    /**
     * Send a message when a player transfers TNT to another player
     * @param receiver The player who received the TNT
     * @param giver The player who gave the TNT
     */
    public void broadcastTntTransferred(Player receiver, Player giver) {
        broadcast(prefixed("&e" + receiver.getName() + "&c ha recibido la TNT de " + "&e" + giver.getName() + "&c!"));

        // Personal messages
        receiver.sendMessage(prefixed("&c¡Has recibido la TNT de " + giver.getName() + "!"));
        giver.sendMessage(prefixed("&a¡" + receiver.getName() + " ha recibido tu TNT!"));

        debug("Transferencia de TNT: " + receiver.getName() + " recibió TNT de " + giver.getName());
    }

    // ===== PLAYER DEATH MESSAGES =====

    /**
     * Send a message when a player dies for having TNT at the end of the game
     * @param player The player who died
     */
    public void sendDeathMessage(Player player) {
        player.sendMessage(prefixed("&c¡Has muerto debido a la explosión de la TNT al final del juego!"));
        broadcast(prefixed("&c" + player.getName() + " ha muerto debido a la explosión de la TNT al final del juego."));

        debug("Muerte de jugador: " + player.getName() + " murió por tener la TNT al final del juego.");
        debug("Estado del jugador antes de morir - OP: " + player.isOp() +
                ", Salud: " + player.getHealth() + ", Nivel: " + player.getLevel());
    }

    /**
     * Send a message when a player's inventory is cleared at the end of the game
     * @param player The player whose inventory was cleared
     */
    public void sendInventoryClearedMessage(Player player) {
        player.sendMessage(prefixed("&e¡Tu inventario ha sido limpiado al finalizar el juego!"));

        debug("Inventario limpiado: " + player.getName() + " tenía la TNT al final del juego.");
    }

    // ===== GAME END MESSAGES =====

    /**
     * Broadcast a message when the game ends
     * @param reason The reason why the game ended (timeout, manual stop, etc.)
     * @param deathCount The number of players who died (eliminated)
     */
    public void broadcastGameEnd(String reason, int deathCount) {
        // Enhanced end game message with more details - sent to all players
        if (reason.equals("Tiempo agotado")) {
            broadcast(prefixed("&c¡La TNT ha explotado! " +
                    "&eRazón: &f" +
                    "El tiempo se agotó."));
        } else {
            broadcast(prefixed("&c¡El minijuego TNT Tag ha terminado! " +
                    "&eRazón: &f" + reason));
        }

        // Count survivors - only for non-OP players
        int nonOpSurvivors = 0;
        int totalNonOp = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOp()) {
                totalNonOp++;
                if (!player.isDead()) {
                    nonOpSurvivors++;
                }
            }
        }

        // Use the provided death count instead of calculating it
        int nonOpDeaths = deathCount;

        // Display statistics with consistent colors - only for non-OP players
        broadcast(prefixed("&e=== Estadísticas Finales (Jugadores) ==="));
        broadcast(prefixed("&a- Sobrevivientes: &a" + nonOpSurvivors +
                "&e jugadores"));
        broadcast(prefixed("&c- Eliminados por TNT: &c" + nonOpDeaths +
                "&e jugadores"));
        broadcast(prefixed("&e- Participación total: &f" + totalNonOp +
                "&e jugadores"));

        debug("Juego finalizado. Razón: " + reason + ". Estadísticas: Sobrevivientes: " +
                nonOpSurvivors + ", Eliminados por TNT: " + nonOpDeaths + ", Total: " + totalNonOp);
    }

    /**
     * Broadcast a message with the list of survivors
     * @param survivors List of players who survived
     */
    public void broadcastSurvivors(java.util.List<Player> survivors) {
        // Filter out OP players for statistics
        java.util.List<Player> nonOpSurvivors = new java.util.ArrayList<>();
        for (Player player : survivors) {
            if (!player.isOp()) {
                nonOpSurvivors.add(player);
            }
        }

        if (nonOpSurvivors.isEmpty()) {
            broadcast(prefixed("&c¡Todos los jugadores han sido eliminados por la TNT!"));
            return;
        }

        // Header for survivors list
        broadcast(prefixed("&e=== Sobrevivientes del TNT Tag ==="));

        // Format survivor names with green color
        StringBuilder survivorNames = new StringBuilder();
        for (int i = 0; i < nonOpSurvivors.size(); i++) {
            if (i > 0 && i % 3 == 0) {
                // Start a new line every 3 names for better readability
                broadcast(getColoredMessage(survivorNames.toString()));
                survivorNames = new StringBuilder();
            } else if (i > 0) {
                survivorNames.append(", ");
            }
            survivorNames.append("&a").append(nonOpSurvivors.get(i).getName()).append("&f");
        }

        // Broadcast the last line of names
        if (survivorNames.length() > 0) {
            broadcast(getColoredMessage(survivorNames.toString()));
        }

        // Footer for survivors list
        broadcast(prefixed("&eTotal: &a" + nonOpSurvivors.size() + "&e jugadores sobrevivieron a la explosión"));

        debug("Lista de sobrevivientes: " + nonOpSurvivors.size() + " jugadores");
    }

    // ===== GENERAL UTILITY METHODS =====

    /**
     * Broadcast a message to all players
     * @param message The message to broadcast
     */
    public void broadcast(String message) {
        Bukkit.broadcastMessage(getColoredMessage(message));
    }

    /**
     * Send a message to the console
     * @param message The message to log
     */
    public void log(String message) {
        logger.info(getColoredMessage(message));
    }

    /**
     * Send a warning message to the console
     * @param message The warning message
     */
    public void warn(String message) {
        logger.warning(getColoredMessage(message));
    }

    /**
     * Send an error message to the console
     * @param message The error message
     */
    public void error(String message) {
        logger.severe(getColoredMessage(message));
    }

    /**
     * Send a debug message to the console if debug mode is enabled
     * @param message The debug message
     */
    public void debug(String message) {
        if (debugMode) {
            logger.log(Level.INFO, getColoredMessage(DEBUG_PREFIX + message));
            // Also send to operators if they're online
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.isOp()) {
                    player.sendMessage(getColoredMessage(DEBUG_PREFIX + message));
                }
            }
        }
    }

    /**
     * Format time in seconds to a readable string (MM:SS)
     * @param totalSeconds Time in seconds
     * @return Formatted time string
     */
    private String formatTime(int totalSeconds) {
        int min = totalSeconds / 60;
        int sec = totalSeconds % 60;
        return String.format("%02d:%02d", min, sec);
    }

    /**
     * Converts a message with '&' color codes to the appropriate ChatColor
     * @param message The message to be converted
     * @return The colored message
     */
    private String getColoredMessage(String message) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
}
