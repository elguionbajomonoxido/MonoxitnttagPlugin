package com.monoxitnttag;

import com.monoxitnttag.commands.MonoxitnttagCommand;
import com.monoxitnttag.commands.MonoxitnttagTabCompleter;
import com.monoxitnttag.placeholders.TntTagPlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import com.monoxitnttag.listeners.TntTagListener;
import com.monoxitnttag.util.ItemStackUtil;
import com.monoxitnttag.managers.MessageManager;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MonoxitnttagPlugin extends JavaPlugin implements Listener {

    // --- Minigame state ---
    private boolean minigameActive = false;
    private int minigamePlayersRequired = 0;
    private int minigameTimeSeconds = 0;
    private BossBar bossBar;
    private BukkitRunnable timerTask;
    private int remainingSeconds;
    private java.util.Set<Player> jugadoresConTnt = new java.util.HashSet<>();

    /**
     * Check if the minigame is currently active
     * @return true if the minigame is active, false otherwise
     */
    public boolean isMinigameActive() {
        return minigameActive;
    }

    // --- Message manager ---
    private MessageManager messageManager;

    /**
     * Get the message manager instance
     * @return The message manager
     */
    public MessageManager getMessageManager() {
        return messageManager;
    }

    // --- Configurable messages ---
    private String msgStart;
    private String msgStop;
    private String msgEnd;
    private String msgNoPerm;
    private String msgNoConfig;

    public String getMsgStart() { return msgStart; }
    public String getMsgStop() { return msgStop; }
    public String getMsgEnd() { return msgEnd; }
    public String getMsgNoPerm() { return msgNoPerm; }
    public String getMsgNoConfig() { return msgNoConfig; }
    public void loadMessages() { loadMessagesInternal(); }
    private void loadMessagesInternal() {
        msgStart = getConfig().getString("mensajes.inicio", "§a¡El minijuego 'TNT Tag' ha comenzado!");
        msgStop = getConfig().getString("mensajes.stop", "§cEl minijuego ha sido detenido.");
        msgEnd = getConfig().getString("mensajes.fin", "§e¡Fin del minijuego!");
        msgNoPerm = getConfig().getString("mensajes.noperm", "§cNo tienes permiso para usar este comando.");
        msgNoConfig = getConfig().getString("mensajes.noconfig", "§cError: El item 'tnt' no es válido o falta en config.yml.");
    }

    @Override
    public void onEnable() {
        // Initialize the message manager
        this.messageManager = new MessageManager(this);
        this.messageManager.setDebugMode(getConfig().getBoolean("debug", false));
        this.messageManager.log("Inicializando plugin MonoxiTntTag...");

        loadMessagesInternal();
        try {
            getLogger().info("Iniciando carga del item de TNT desde config.yml...");
            ItemStackUtil.loadTntItem(getConfig());

            // Validar que el item se cargó correctamente
            ItemStack tntItem = ItemStackUtil.getTntItem();
            if (tntItem != null) {
                getLogger().info("Item de TNT cargado: " + tntItem.getType().name());
                if (tntItem.hasItemMeta() && tntItem.getItemMeta().hasDisplayName()) {
                    getLogger().info("Nombre del item: " + tntItem.getItemMeta().getDisplayName());
                } else {
                    getLogger().warning("El item no tiene nombre configurado.");
                }
            } else {
                getLogger().severe("¡El item de TNT es null después de cargarlo!");

                // Crear un ítem predeterminado si no se pudo cargar
                ItemStack defaultItem = new ItemStack(org.bukkit.Material.TNT, 1);
                org.bukkit.inventory.meta.ItemMeta meta = defaultItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§c§lTNT Activa");

                    // Establecer lore
                    java.util.List<String> lore = new java.util.ArrayList<>();
                    lore.add("§f§l¡Esta TNT está activa!");
                    lore.add("§f§lPásala a otro jugador");
                    lore.add("§c§lantes de que explote...");
                    meta.setLore(lore);

                    defaultItem.setItemMeta(meta);
                    ItemStackUtil.setTntItem(defaultItem);
                    getLogger().info("Se ha creado un ítem de TNT predeterminado.");
                }
            }
        } catch (Exception e) {
            getLogger().severe("Error cargando el item de TNT: " + e.getMessage());
            e.printStackTrace();
            getLogger().severe("El plugin NO se deshabilitará, pero el minijuego no funcionará correctamente hasta que config.yml sea válido.");

            // Crear un ítem predeterminado en caso de error
            ItemStack defaultItem = new ItemStack(org.bukkit.Material.TNT, 1);
            org.bukkit.inventory.meta.ItemMeta meta = defaultItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§c§lTNT Activa");

                // Establecer lore
                java.util.List<String> lore = new java.util.ArrayList<>();
                lore.add("§f§l¡Esta TNT está activa!");
                lore.add("§f§lPásala a otro jugador");
                lore.add("§c§lantes de que explote...");
                meta.setLore(lore);

                defaultItem.setItemMeta(meta);
                ItemStackUtil.setTntItem(defaultItem);
                getLogger().info("Se ha creado un ítem de TNT predeterminado debido a un error.");
            }
        }

        // Registrar comandos y listeners
        getCommand("MonoxiTntTag").setExecutor(new MonoxitnttagCommand(this));
        getCommand("MonoxiTntTag").setTabCompleter(new MonoxitnttagTabCompleter());
        Bukkit.getPluginManager().registerEvents(new TntTagListener(this), this);
        Bukkit.getPluginManager().registerEvents(this, this); // Para PlayerJoinEvent

        // Register PlaceholderAPI expansion if available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            messageManager.debug("PlaceholderAPI encontrado, registrando expansión de placeholders...");
            new TntTagPlaceholder(this).register();
            messageManager.debug("Expansión de placeholders registrada correctamente.");
        } else {
            messageManager.debug("PlaceholderAPI no encontrado, los placeholders no estarán disponibles.");
        }
    }

    @Override
    public void onDisable() {
        if (bossBar != null) bossBar.removeAll();
        if (timerTask != null) timerTask.cancel();
    }

    // --- Minigame logic ---
    public void startMinigame(int cantidadJugadoresConTnt, int tiempo) {
        this.minigameActive = true;
        this.minigamePlayersRequired = cantidadJugadoresConTnt;
        this.minigameTimeSeconds = tiempo;
        this.remainingSeconds = tiempo;

        // Debug message for game initialization
        messageManager.debug("Iniciando minijuego con parámetros: jugadoresConTnt=" + cantidadJugadoresConTnt + ", tiempo=" + tiempo + " segundos");

        // Dar la "TNT" a jugadores, priorizando a los que no tienen OP
        java.util.List<Player> online = new java.util.ArrayList<>(Bukkit.getOnlinePlayers());

        // Separar jugadores en dos listas: con OP y sin OP
        java.util.List<Player> jugadoresSinOP = new java.util.ArrayList<>();
        java.util.List<Player> jugadoresConOP = new java.util.ArrayList<>();

        for (Player player : online) {
            if (player.isOp()) {
                jugadoresConOP.add(player);
            } else {
                jugadoresSinOP.add(player);
            }
        }

        // Verificar si hay suficientes jugadores para el juego
        if (jugadoresSinOP.isEmpty()) {
            messageManager.broadcast(messageManager.prefixed("&cNo hay jugadores conectados. El juego requiere al menos un jugador sin OP."));
            messageManager.debug("No se pudo iniciar el juego: no hay jugadores conectados");
            minigameActive = false;
            return;
        }

        messageManager.debug("Jugadores disponibles: " + online.size() + " total, " + jugadoresSinOP.size() + " sin OP, " + jugadoresConOP.size() + " con OP");

        // Calcular cuántos jugadores deben tener TNT y cuántos no
        // cantidadJugadoresConTnt indica cuántos jugadores tendrán TNT
        int jugadoresSinTnt;
        int jugadoresConTnt;

        if (cantidadJugadoresConTnt == 1) {
            // Si se especificó exactamente 1 jugador con TNT, asegurar que sea así
            jugadoresConTnt = 1;
            // El resto de jugadores no tendrán TNT
            jugadoresSinTnt = jugadoresSinOP.size() - 1;

            // Si no hay suficientes jugadores sin OP, ajustar
            if (jugadoresSinTnt < 0) {
                // Si solo hay un jugador sin OP, todos tendrán TNT
                jugadoresConTnt = jugadoresSinOP.size();
                jugadoresSinTnt = 0;
                messageManager.broadcast(messageManager.prefixed("&eAviso: Solo hay un jugador sin OP, por lo que todos tendrán TNT."));
                messageManager.debug("Ajuste automático: Solo hay un jugador sin OP, todos tendrán TNT");
            }
        } else {
            // Cálculo normal para otros valores
            jugadoresConTnt = Math.min(cantidadJugadoresConTnt, jugadoresSinOP.size());

            // Asegurar que al menos un jugador tenga TNT
            if (jugadoresConTnt < 1 && jugadoresSinOP.size() > 0) {
                jugadoresConTnt = 1;
            }

            jugadoresSinTnt = jugadoresSinOP.size() - jugadoresConTnt;

            // Asegurar que al menos un jugador no tenga TNT si hay más de uno
            if (jugadoresSinTnt < 0) {
                jugadoresSinTnt = 0;
                jugadoresConTnt = jugadoresSinOP.size();
            }
        }

        if (jugadoresConTnt < jugadoresSinOP.size()) {
            messageManager.broadcast(messageManager.prefixed("&eJugadores con TNT: &f" + jugadoresConTnt + "&e. Jugadores sin TNT: &f" + jugadoresSinTnt + "&e."));
        } else {
            messageManager.broadcast(messageManager.prefixed("&eTodos los jugadores recibirán la TNT."));
        }

        // Preparar para el inicio del juego
        messageManager.broadcastGameStart(tiempo, jugadoresConTnt, jugadoresSinTnt);

        // Mezclar las listas para asignación aleatoria dentro de cada grupo
        java.util.Collections.shuffle(jugadoresSinOP);
        java.util.Collections.shuffle(jugadoresConOP);

        this.jugadoresConTnt.clear();
        ItemStack tntItem = com.monoxitnttag.util.ItemStackUtil.getTntItem();
        if (tntItem == null) {
            messageManager.broadcast(messageManager.prefixed("&cError: El item 'tnt' no está configurado correctamente."));
            messageManager.error("El item 'tnt' es null. No se puede iniciar el minijuego.");
            minigameActive = false;
            return;
        }

        messageManager.debug("Item de TNT cargado correctamente: " + tntItem.getType().name() + 
                           (tntItem.hasItemMeta() && tntItem.getItemMeta().hasDisplayName() ? 
                           " con nombre: " + tntItem.getItemMeta().getDisplayName() : ""));

        // Primero asignar TNT a jugadores sin OP
        int tntAsignadas = 0;
        for (Player p : jugadoresSinOP) {
            if (tntAsignadas >= jugadoresConTnt) {
                break; // Ya asignamos todas las TNT necesarias
            }

            // Asignar TNT a este jugador sin OP
            try {
                p.getInventory().setHelmet(tntItem.clone());
            } catch (Exception e) {
                // Si Bukkit falla, usar NMS para forzar el item en la cabeza
                try {
                    org.bukkit.inventory.PlayerInventory inv = p.getInventory();
                    java.lang.reflect.Method setItem = inv.getClass().getMethod("setItem", int.class, ItemStack.class);
                    setItem.invoke(inv, 39, tntItem.clone()); // 39 = helmet slot
                } catch (Exception ignored) {
                    messageManager.debug("Error al asignar TNT a " + p.getName() + " usando NMS: " + ignored.getMessage());
                }
            }
            this.jugadoresConTnt.add(p);
            // Apply golden glow effect to player with TNT
            applyGlowEffect(p);
            tntAsignadas++;

            // Enviar mensaje personalizado al jugador
            messageManager.sendEarsReceived(p);
        }

        // Si aún faltan TNT por asignar, asignarlas a jugadores con OP
        if (tntAsignadas < jugadoresConTnt) {
            for (Player p : jugadoresConOP) {
                if (tntAsignadas >= jugadoresConTnt) {
                    break; // Ya asignamos todas las TNT necesarias
                }

                // Asignar TNT a este jugador con OP
                try {
                    p.getInventory().setHelmet(tntItem.clone());
                } catch (Exception e) {
                    // Si Bukkit falla, usar NMS para forzar el item en la cabeza
                    try {
                        org.bukkit.inventory.PlayerInventory inv = p.getInventory();
                        java.lang.reflect.Method setItem = inv.getClass().getMethod("setItem", int.class, ItemStack.class);
                        setItem.invoke(inv, 39, tntItem.clone()); // 39 = helmet slot
                    } catch (Exception ignored) {
                        messageManager.debug("Error al asignar TNT a " + p.getName() + " (OP) usando NMS: " + ignored.getMessage());
                    }
                }
                this.jugadoresConTnt.add(p);
                // Apply golden glow effect to player with TNT
                applyGlowEffect(p);
                tntAsignadas++;

                // Enviar mensaje personalizado al jugador con OP
                messageManager.sendEarsReceived(p);
                messageManager.debug("Jugador con OP " + p.getName() + " recibió TNT porque no había suficientes jugadores sin OP");
            }
        }

        // Enviar mensajes a jugadores que no recibieron TNT
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!this.jugadoresConTnt.contains(p)) {
                messageManager.sendNoEarsWarning(p);
                messageManager.debug("Jugador " + p.getName() + " no recibió TNT al inicio del juego");
            }
        }

        // Crear BossBar
        if (bossBar != null) bossBar.removeAll();
        bossBar = Bukkit.createBossBar("Tiempo restante: " + formatTime(remainingSeconds), BarColor.BLUE, BarStyle.SOLID);
        bossBar.setProgress(1.0);
        for (Player p : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(p);
        }
        // Timer
        if (timerTask != null) timerTask.cancel();
        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                remainingSeconds--;

                // Actualizar la barra de progreso
                bossBar.setTitle("Tiempo restante: " + formatTime(remainingSeconds));
                bossBar.setProgress(Math.max(0, (double)remainingSeconds / minigameTimeSeconds));

                // Enviar mensajes de actualización de tiempo a intervalos específicos
                messageManager.broadcastTimeUpdate(remainingSeconds, minigameTimeSeconds);

                // Verificar si el juego ha terminado
                if (remainingSeconds <= 0) {
                    bossBar.setTitle("¡Fin del minijuego!");
                    bossBar.setProgress(0);
                    bossBar.removeAll();
                    cancel();
                    minigameActive = false;

                    // Recopilar información de jugadores para estadísticas
                    java.util.List<Player> survivors = new java.util.ArrayList<>();
                    int eliminados = 0;

                    // Matar a los jugadores que tienen TNT
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (!player.isOp() && hasTnt(player)) {
                            player.setHealth(0);
                            messageManager.sendDeathMessage(player);
                            eliminados++;
                        } else {
                            survivors.add(player);
                        }
                    }

                    // Anunciar el fin del juego
                    messageManager.broadcastGameEnd("Tiempo agotado", eliminados);

                    // Limpiar los inventarios de los jugadores con TNT
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (hasTnt(player)) {
                            player.getInventory().clear();
                            // Remove glow effect
                            removeGlowEffect(player);
                            messageManager.sendInventoryClearedMessage(player);
                        }
                    }

                    // Mostrar lista de sobrevivientes
                    messageManager.broadcastSurvivors(survivors);

                    // Mensaje de debug con estadísticas finales
                    messageManager.debug("Juego finalizado por tiempo. Estadísticas finales: " + 
                                       survivors.size() + " sobrevivientes, " + 
                                       (Bukkit.getOnlinePlayers().size() - survivors.size()) + " eliminados");
                }
            }
        };
        timerTask.runTaskTimer(this, 20, 20);
    }

    public void stopMinigame() {
        minigameActive = false;
        if (bossBar != null) bossBar.removeAll();
        if (timerTask != null) timerTask.cancel();

        messageManager.debug("Deteniendo minijuego manualmente");
        messageManager.broadcast(messageManager.prefixed("&cEl minijuego ha sido detenido manualmente."));

        // Recopilar información de jugadores para estadísticas
        java.util.List<Player> survivors = new java.util.ArrayList<>();
        int eliminados = 0;

        // Matar a los jugadores que tienen TNT
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOp() && hasTnt(player)) {
                messageManager.debug("Eliminando a " + player.getName() + " por tener TNT al final del juego");
                player.setHealth(0);
                messageManager.sendDeathMessage(player);
                eliminados++;
            } else {
                survivors.add(player);
            }
        }

        // Quitar la "TNT" a todos los jugadores que la tengan y limpiar sus inventarios
        for (Player p : Bukkit.getOnlinePlayers()) {
            // Verificar si el jugador tiene la TNT en este momento
            if (hasTnt(p)) {
                messageManager.debug("Limpiando inventario de " + p.getName() + " que tenía TNT al final del juego");
                // Limpiar todo el inventario, incluyendo el casco
                p.getInventory().clear();
                // Remove glow effect
                removeGlowEffect(p);
                messageManager.sendInventoryClearedMessage(p);
            }
        }

        // Anunciar el fin del juego y mostrar estadísticas
        messageManager.broadcastGameEnd("Detenido manualmente", eliminados);
        messageManager.broadcastSurvivors(survivors);

        // Mensaje de debug con estadísticas finales
        messageManager.debug("Juego finalizado manualmente. Estadísticas finales: " + 
                           survivors.size() + " sobrevivientes, " + 
                           (Bukkit.getOnlinePlayers().size() - survivors.size()) + " eliminados");

        jugadoresConTnt.clear();
    }

    private String formatTime(int totalSeconds) {
        int min = totalSeconds / 60;
        int sec = totalSeconds % 60;
        return String.format("%02d:%02d", min, sec);
    }

    /**
     * Check if a player has the TNT item
     * @param player The player to check
     * @return true if the player has the TNT, false otherwise
     */
    public boolean hasTnt(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack tntItem = com.monoxitnttag.util.ItemStackUtil.getTntItem();
        if (helmet == null || tntItem == null) return false;

        // Verificar que el tipo de material sea el mismo
        if (helmet.getType() != tntItem.getType()) return false;

        // Verificar si ambos tienen ItemMeta
        if (helmet.hasItemMeta() && tntItem.hasItemMeta()) {
            ItemMeta helmetMeta = helmet.getItemMeta();
            ItemMeta tntMeta = tntItem.getItemMeta();

            // Verificar si tienen el mismo nombre
            if (helmetMeta.hasDisplayName() && tntMeta.hasDisplayName()) {
                if (!helmetMeta.getDisplayName().equals(tntMeta.getDisplayName())) {
                    return false;
                }
            } else {
                // Si uno tiene nombre y el otro no, no son iguales
                if (helmetMeta.hasDisplayName() != tntMeta.hasDisplayName()) {
                    return false;
                }
            }

            // Verificar si tienen el mismo lore
            if (helmetMeta.hasLore() && tntMeta.hasLore()) {
                return helmetMeta.getLore().equals(tntMeta.getLore());
            }
        }

        // Si llegamos aquí, comparar con isSimilar() como fallback
        return helmet.isSimilar(tntItem);
    }

    /**
     * Update a player's TNT status
     * @param player The player to update
     * @param hasTnt true if the player should have TNT, false otherwise
     */
    public void updateTntStatus(Player player, boolean hasTnt) {
        ItemStack tntItem = com.monoxitnttag.util.ItemStackUtil.getTntItem();
        if (hasTnt) {
            if (tntItem != null) player.getInventory().setHelmet(tntItem.clone());
            jugadoresConTnt.add(player);
            // Apply red glow effect to player with TNT
            applyGlowEffect(player);
        } else {
            player.getInventory().setHelmet(null);
            jugadoresConTnt.remove(player);
            // Remove glow effect from player without TNT
            removeGlowEffect(player);
        }
    }

    /**
     * Legacy method for backward compatibility
     * @deprecated Use hasTnt instead
     */
    @Deprecated
    public boolean hasEars(Player player) {
        return hasTnt(player);
    }

    /**
     * Legacy method for backward compatibility
     * @deprecated Use updateTntStatus instead
     */
    @Deprecated
    public void updateEarsStatus(Player player, boolean hasEars) {
        updateTntStatus(player, hasEars);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (minigameActive && bossBar != null) {
            bossBar.addPlayer(event.getPlayer());
        }
    }

    public void reloadMinigameConfig() {
        ItemStackUtil.loadTntItem(getConfig());
    }

    /**
     * Apply golden glow effect to a player
     * @param player The player to apply the glow effect to
     */
    public void applyGlowEffect(Player player) {
        // Apply infinite glowing effect
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));

        // Set up scoreboard team with golden color for the glow effect
        org.bukkit.scoreboard.Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        org.bukkit.scoreboard.Team team = scoreboard.getTeam("goldenGlow");

        if (team == null) {
            team = scoreboard.registerNewTeam("goldenGlow");
            team.setColor(org.bukkit.ChatColor.GOLD); // Set golden color
            messageManager.debug("Created golden glow team");
        }

        // Add player to the team to get the golden glow color
        team.addEntry(player.getName());

        messageManager.debug("Applied golden glow effect to " + player.getName());
    }

    /**
     * Remove glow effect from a player
     * @param player The player to remove the glow effect from
     */
    public void removeGlowEffect(Player player) {
        // Remove glowing effect
        player.removePotionEffect(PotionEffectType.GLOWING);

        // Remove player from the golden glow team
        org.bukkit.scoreboard.Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        org.bukkit.scoreboard.Team team = scoreboard.getTeam("goldenGlow");

        if (team != null && team.hasEntry(player.getName())) {
            team.removeEntry(player.getName());
            messageManager.debug("Removed player from golden glow team: " + player.getName());
        }

        messageManager.debug("Removed glow effect from " + player.getName());
    }
}
