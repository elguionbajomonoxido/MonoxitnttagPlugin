package com.monoxitnttag.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.logging.Logger;

public class ItemStackUtil {

    private static ItemStack tntItem;
    private static Logger logger;

    /**
     * Inicializa el logger para los mensajes de depuración
     *
     * @param pluginLogger El logger del plugin
     */
    public static void setLogger(Logger pluginLogger) {
        logger = pluginLogger;
    }

    /**
     * Carga el item de TNT directamente desde el código base
     * en lugar de leerlo desde la configuración
     *
     * @param config La configuración del plugin (no se usa, mantenido por compatibilidad)
     */
    public static void loadTntItem(FileConfiguration config) {
        if (logger != null) {
            logger.info("Creando item de TNT directamente desde el código base...");
        }

        // Crear el item base
        tntItem = new ItemStack(org.bukkit.Material.TNT, 1);

        // Configurar el ItemMeta
        ItemMeta meta = tntItem.getItemMeta();
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
            tntItem.setItemMeta(meta);
        }

        if (logger != null) {
            logger.info("Item de TNT creado exitosamente desde el código base.");
            logger.info("Tipo: " + tntItem.getType().name());
        }
    }

    public static ItemStack getTntItem() {
        return tntItem != null ? tntItem.clone() : null;
    }

    /**
     * Establece el item de TNT
     *
     * @param itemStack El nuevo ItemStack a usar como TNT
     */
    public static void setTntItem(ItemStack itemStack) {
        tntItem = (itemStack != null) ? itemStack.clone() : null;
    }

}
