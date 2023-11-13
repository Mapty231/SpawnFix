package me.tye.spawnfix;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerJoin implements Listener {

private static final ArrayList<UUID> joined = new ArrayList<>();
public static JavaPlugin plugin = JavaPlugin.getPlugin(SpawnFix.class);

@EventHandler
public static void PlayerSpawn(PlayerJoinEvent e) {
    Player player = e.getPlayer();

    if (joined.contains(player.getUniqueId())) {
        return;
    }

    PersistentDataContainer dataContainer = player.getPersistentDataContainer();

    Double lastLoinX = dataContainer.get(new NamespacedKey(plugin, "lastloginx"), PersistentDataType.DOUBLE);
    Double lastLoinY = dataContainer.get(new NamespacedKey(plugin, "lastloginy"), PersistentDataType.DOUBLE);
    Double lastLoinZ = dataContainer.get(new NamespacedKey(plugin, "lastloginz"), PersistentDataType.DOUBLE);

    Location properLocation;

    //if there is an error reading the data or the player hasn't joined before, use the default location.
    if (lastLoinX == null || lastLoinY == null || lastLoinZ == null) {
        try {
            double defaultX = Double.parseDouble(String.valueOf(plugin.getConfig().get("default.x")));
            double defaultY = Double.parseDouble(String.valueOf(plugin.getConfig().get("default.y")));
            double defaultZ = Double.parseDouble(String.valueOf(plugin.getConfig().get("default.z")));

            properLocation = new Location(player.getWorld(), defaultX, defaultY, defaultZ);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Could not parse entered value for default spawn.");
            return;
        }
    }
    //if the player has joined before, get the last join location.
    else {
        properLocation = new Location(player.getWorld(), lastLoinX, lastLoinY, lastLoinZ);
    }


    while (!player.getLocation().equals(properLocation)) {
        player.teleport(properLocation);
    }

    joined.add(player.getUniqueId());
}
}