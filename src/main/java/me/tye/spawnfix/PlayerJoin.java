package me.tye.spawnfix;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

import static me.tye.spawnfix.Util.get;
import static me.tye.spawnfix.Util.plugin;

public class PlayerJoin implements Listener {

private static final ArrayList<UUID> joined = new ArrayList<>();

@EventHandler
public static void PlayerSpawn(PlayerJoinEvent e) {
    Player player = e.getPlayer();

    String login = get("login");

    //if login is never then always return.
    if (login.equals("never")) {
        return;
    }

    //if the login is first, only teleport on the first join.
    if (login.equals("first") && joined.contains(player.getUniqueId())) {
        return;
    }

    PersistentDataContainer dataContainer = player.getPersistentDataContainer();

    Double lastLoginX = dataContainer.get(new NamespacedKey(plugin, "lastloginx"), PersistentDataType.DOUBLE);
    Double lastLoginY = dataContainer.get(new NamespacedKey(plugin, "lastloginy"), PersistentDataType.DOUBLE);
    Double lastLoginZ = dataContainer.get(new NamespacedKey(plugin, "lastloginz"), PersistentDataType.DOUBLE);
    String lastLoginWorldName = dataContainer.get(new NamespacedKey(plugin, "lastloginworld"), PersistentDataType.STRING);

    Location properLocation;

    //if there is an error reading the data or the player hasn't joined before, use the default location.
    if (lastLoginX == null || lastLoginY == null || lastLoginZ == null || lastLoginWorldName == null) {
        try {
            double defaultX = Double.parseDouble(get("default.x"));
            double defaultY = Double.parseDouble(get("default.y"));
            double defaultZ = Double.parseDouble(get("default.z"));

            properLocation = new Location(Bukkit.getWorld(get("default.worldName")), defaultX, defaultY, defaultZ);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Could not parse entered value for default spawn.");
            return;
        }
    }
    //if the player has joined before, get the last join location.
    else {
        properLocation = new Location(Bukkit.getWorld(lastLoginWorldName), lastLoginX, lastLoginY, lastLoginZ);
    }

    int retryInterval = 2;
    try {
        retryInterval = Integer.parseInt(get("teleport.retryInterval"));
    } catch (NumberFormatException ex) {
        plugin.getLogger().log(Level.WARNING, "Unable to parse the retry interval, defaulting to 2.");
    }

    BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, new Teleport(player, properLocation), 2, retryInterval);
    Teleport.runningTasks.put(player.getUniqueId(), bukkitTask);

    joined.add(player.getUniqueId());
}
}
