package com.monoxitnttag.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Sound;
import com.monoxitnttag.MonoxitnttagPlugin;
import com.monoxitnttag.managers.MessageManager;

public class TntTagListener implements Listener {

    private final MonoxitnttagPlugin plugin;

    public TntTagListener(MonoxitnttagPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            Player victim = (Player) event.getEntity();

            // Verificar si el juego está activo
            if (!plugin.isMinigameActive()) {
                return;
            }

            ItemStack victimTnt = victim.getInventory().getHelmet();
            if (plugin.hasEars(victim)) {  // Using existing method for compatibility
                // Transfer TNT from victim to attacker
                victim.getInventory().setHelmet(null);
                attacker.getInventory().setHelmet(victimTnt);

                // Usar el MessageManager para enviar mensajes
                MessageManager messageManager = plugin.getMessageManager();
                messageManager.broadcastTntTransferred(attacker, victim);

                // Actualizar el estado del TNT en el plugin
                plugin.updateEarsStatus(attacker, true);  // Using existing method for compatibility
                plugin.updateEarsStatus(victim, false);   // Using existing method for compatibility

                // Reproducir sonido de TNT para todos los jugadores cercanos
                attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_TNT_PRIMED, 1.0f, 1.0f);

                // Mensaje de debug
                messageManager.debug("Evento de transferencia de TNT: " + attacker.getName() + " recibió TNT de " + victim.getName() + 
                                   ". Daño causado: " + event.getDamage());
            }
        }
    }
}
