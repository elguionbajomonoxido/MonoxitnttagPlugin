package com.monoxitnttag.commands;

import com.monoxitnttag.MonoxitnttagPlugin;
import com.monoxitnttag.managers.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class MonoxitnttagCommand implements CommandExecutor {
    private final MonoxitnttagPlugin plugin;
    public MonoxitnttagCommand(MonoxitnttagPlugin plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§eUsa /MonoxiTntTag <start|setitem|reset|reload|stop> ...");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "start":
                if (!(sender instanceof org.bukkit.entity.Player) && !sender.isOp()) {
                    sender.sendMessage(plugin.getMsgNoPerm());
                    return true;
                }
                if (args.length != 3) {
                    sender.sendMessage("§cUso correcto: /MonoxiTntTag start <cantidadJugadoresConTnt> <tiempo(minutos:segundos)>");
                    return true;
                }
                try {
                    int cantidadJugadoresConTnt = Integer.parseInt(args[1]);
                    int tiempo = parseTime(args[2]);

                    // Obtener el MessageManager
                    MessageManager messageManager = plugin.getMessageManager();
                    messageManager.debug("Comando start ejecutado por " + sender.getName() + 
                                       " con parámetros: jugadoresConTnt=" + cantidadJugadoresConTnt + 
                                       ", tiempo=" + tiempo + " segundos");

                    plugin.startMinigame(cantidadJugadoresConTnt, tiempo);
                    messageManager.broadcast(plugin.getMsgStart());
                } catch (Exception e) {
                    sender.sendMessage("§cPor favor, introduce valores válidos.");
                }
                return true;
            case "setitem":
                if (!(sender instanceof org.bukkit.entity.Player) && !sender.isOp()) {
                    sender.sendMessage(plugin.getMsgNoPerm());
                    return true;
                }
                // Nuevo: sin argumentos, toma el item de la mano principal
                if (args.length != 2) {
                    org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
                    org.bukkit.inventory.ItemStack itemEnMano = player.getInventory().getItemInMainHand();
                    if (itemEnMano == null || itemEnMano.getType().isAir()) {
                        sender.sendMessage("Debes tener un item en la mano principal para usar este comando.");
                        return true;
                    }

                    // Guardar una copia del ítem para evitar problemas de referencia
                    org.bukkit.inventory.ItemStack itemGuardado = itemEnMano.clone();

                    try {
                        // Guardar el ítem con el formato correcto
                        guardarItemConFormatoExacto(plugin, itemGuardado);

                        // Actualizar el ítem en memoria para uso inmediato
                        com.monoxitnttag.util.ItemStackUtil.setTntItem(itemGuardado);

                        // Recargar la configuración para asegurar que los cambios se apliquen correctamente
                        plugin.reloadConfig();
                        plugin.reloadMinigameConfig();

                        // Obtener el MessageManager
                        MessageManager messageManager = plugin.getMessageManager();

                        // Enviar mensajes
                        sender.sendMessage("El item de orejas ha sido actualizado con el item en tu mano principal.");
                        messageManager.broadcast("§6" + sender.getName() + " §eha actualizado el item de orejas.");

                        // Mensaje de debug
                        messageManager.debug("Item de orejas actualizado por " + sender.getName() + 
                                           " con tipo: " + itemGuardado.getType().name() + 
                                           (itemGuardado.hasItemMeta() && itemGuardado.getItemMeta().hasDisplayName() ? 
                                           ", Nombre: " + itemGuardado.getItemMeta().getDisplayName() : ""));
                    } catch (Exception e) {
                        sender.sendMessage("§cError al guardar el item: " + e.getMessage());
                        plugin.getLogger().severe("Error al guardar el item de orejas: " + e.getMessage());
                        e.printStackTrace();
                    }
                    return true;
                }

                // Compatibilidad: si se pasa un argumento, actualizar el tipo del ítem directamente en memoria
                try {
                    // Obtener el ítem actual
                    org.bukkit.inventory.ItemStack currentItem = com.monoxitnttag.util.ItemStackUtil.getTntItem();

                    if (currentItem == null) {
                        // Si no existe el ítem, crear uno predeterminado
                        crearConfiguracionHardcoded(plugin);
                        currentItem = com.monoxitnttag.util.ItemStackUtil.getTntItem();
                    }

                    // Intentar actualizar el tipo del ítem
                    try {
                        org.bukkit.Material newType = org.bukkit.Material.valueOf(args[1].toUpperCase());

                        // Crear un nuevo ítem con el tipo actualizado
                        org.bukkit.inventory.ItemStack newItem = new org.bukkit.inventory.ItemStack(newType, 1);

                        // Copiar el meta del ítem actual si existe
                        if (currentItem.hasItemMeta()) {
                            newItem.setItemMeta(currentItem.getItemMeta().clone());
                        }

                        // Actualizar el ítem en memoria
                        com.monoxitnttag.util.ItemStackUtil.setTntItem(newItem);

                        // Obtener el MessageManager
                        MessageManager messageManager = plugin.getMessageManager();

                        // Enviar mensajes
                        sender.sendMessage("§aEl tipo del ítem orejas ha sido actualizado a: " + newType.name());
                        messageManager.broadcast("§6" + sender.getName() + " §eha actualizado el tipo del ítem orejas a §b" + newType.name() + "§e.");

                        // Mensaje de debug
                        messageManager.debug("Tipo del ítem orejas actualizado por " + sender.getName() + " a " + newType.name());
                    } catch (IllegalArgumentException ex) {
                        sender.sendMessage("§cTipo de material inválido: " + args[1]);
                        return true;
                    }
                } catch (Exception e) {
                    sender.sendMessage("§cError al actualizar el tipo del item: " + e.getMessage());
                    plugin.getLogger().severe("Error al actualizar el tipo del item: " + e.getMessage());
                    e.printStackTrace();
                }
                return true;
            case "reset":
                if (!sender.hasPermission("monoxitnttag.reset") && !sender.isOp()) {
                    sender.sendMessage(plugin.getMsgNoPerm());
                    return true;
                }
                // Obtener el MessageManager
                MessageManager messageManager = plugin.getMessageManager();
                messageManager.broadcast("El minijuego ha sido reiniciado por " + sender.getName() + ".");
                messageManager.debug("Minijuego reiniciado por " + sender.getName());
                return true;
            case "reload":
                if (!sender.isOp() && !sender.hasPermission("monoxitnttag.reload")) {
                    sender.sendMessage(plugin.getMsgNoPerm());
                    return true;
                }
                try {
                    // Recargar configuración para mensajes y otros ajustes
                    plugin.reloadConfig();
                    plugin.loadMessages();

                    // Recargar el ítem de orejas (ahora se crea directamente en memoria)
                    plugin.reloadMinigameConfig();

                    // Verificar que el ítem se haya cargado correctamente
                    org.bukkit.inventory.ItemStack orejas = com.monoxitnttag.util.ItemStackUtil.getTntItem();
                    if (orejas == null) {
                        // Si el ítem no existe, crear uno predeterminado
                        crearConfiguracionHardcoded(plugin);
                        sender.sendMessage("§eEl ítem de orejas no existía y se ha creado uno predeterminado.");
                    } else {
                        sender.sendMessage("§aConfiguración recargada correctamente.");
                        if (orejas.hasItemMeta() && orejas.getItemMeta().hasDisplayName()) {
                            sender.sendMessage("§aÍtem de orejas: " + orejas.getType().name() + " con nombre: " + orejas.getItemMeta().getDisplayName());
                        } else {
                            sender.sendMessage("§aÍtem de orejas: " + orejas.getType().name());
                        }
                    }
                } catch (Exception e) {
                    sender.sendMessage("§cError crítico al recargar configuración: " + e.getMessage());
                    plugin.getLogger().severe("Error al recargar configuración: " + e.getMessage());
                    e.printStackTrace();
                }
                return true;
            case "stop":
                if (!sender.hasPermission("monoxitnttag.stop") && !sender.isOp()) {
                    sender.sendMessage(plugin.getMsgNoPerm());
                    return true;
                }
                plugin.stopMinigame();
                sender.sendMessage(plugin.getMsgStop());
                return true;
            default:
                sender.sendMessage("§cSubcomando desconocido. Usa /MonoxiTntTag <start|setitem|reset|reload|stop>");
                return true;
        }
    }
    private int parseTime(String time) {
        String[] parts = time.split(":");
        if (parts.length != 2) throw new IllegalArgumentException("Formato de tiempo incorrecto.");
        int minutos = Integer.parseInt(parts[0]);
        int segundos = Integer.parseInt(parts[1]);
        return minutos * 60 + segundos;
    }

    /**
     * Actualiza el ítem directamente en memoria sin guardar en la configuración
     */
    private void guardarItemConFormatoExacto(MonoxitnttagPlugin plugin, org.bukkit.inventory.ItemStack item) {
        try {
            // Asegurarse de que el ítem tenga los metadatos necesarios
            if (item.hasItemMeta()) {
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

                // No necesitamos CustomModelData

                // Asegurar que tenga nombre si no lo tiene
                if (!meta.hasDisplayName()) {
                    meta.setDisplayName("§4§lTNT");
                }

                // Asegurar que tenga lore si no lo tiene
                if (!meta.hasLore() || meta.getLore() == null || meta.getLore().isEmpty()) {
                    java.util.List<String> lore = new java.util.ArrayList<>();
                    lore.add("§f§l¡Esta TNT está activa!");
                    lore.add("§f§lPásala a otro jugador");
                    lore.add("§c§lantes de que explote...");
                    meta.setLore(lore);
                }

                // Aplicar los cambios al ítem
                item.setItemMeta(meta);
            } else {
                // Si no tiene meta, crear uno completo
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§c§lTNT Activa");

                    java.util.List<String> lore = new java.util.ArrayList<>();
                    lore.add("§f§l¡Esta TNT está activa!");
                    lore.add("§f§lPásala a otro jugador");
                    lore.add("§c§lantes de que explote...");
                    meta.setLore(lore);

                    item.setItemMeta(meta);
                }
            }

            // Actualizar el ítem en memoria directamente
            com.monoxitnttag.util.ItemStackUtil.setTntItem(item);

            plugin.getLogger().info("Item actualizado correctamente en memoria.");

        } catch (Exception e) {
            plugin.getLogger().severe("Error al actualizar el ítem en memoria: " + e.getMessage());
            e.printStackTrace();

            // Método de respaldo: crear un ítem predeterminado
            try {
                crearConfiguracionHardcoded(plugin);
            } catch (Exception ex) {
                plugin.getLogger().severe("También falló el método de respaldo: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    /**
     * Método de último recurso: crea un ítem predeterminado directamente en memoria
     */
    private void crearConfiguracionHardcoded(MonoxitnttagPlugin plugin) {
        // Crear un ítem predeterminado
        org.bukkit.inventory.ItemStack defaultItem = new org.bukkit.inventory.ItemStack(org.bukkit.Material.TNT, 1);
        org.bukkit.inventory.meta.ItemMeta meta = defaultItem.getItemMeta();

        if (meta != null) {
            // Establecer nombre
            meta.setDisplayName("§c§lTNT Activa");

            // Establecer lore
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("§f§l¡Esta TNT está activa!");
            lore.add("§f§lPásala a otro jugador");
            lore.add("§c§lantes de que explote...");
            meta.setLore(lore);

            // Aplicar el meta al item
            defaultItem.setItemMeta(meta);

            // Actualizar el ítem en memoria
            com.monoxitnttag.util.ItemStackUtil.setTntItem(defaultItem);

            plugin.getLogger().info("Se ha creado un ítem predeterminado directamente en memoria como último recurso.");
        }
    }
}
