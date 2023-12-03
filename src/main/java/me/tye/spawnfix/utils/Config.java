package me.tye.spawnfix.utils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;

import static me.tye.spawnfix.utils.Util.log;

public enum Config {

  default_x,
  default_y,
  default_z,
  default_worldName,

  teleport_times,
  teleport_retryInterval,

  login,
  onSpawn,
  lang;


/**
 Stores the configs for this plugin.
 */
private static final HashMap<Config, Object> configs = new HashMap<>();


/**
 * @return Gets the config response for the selected enum.
 */
public @NotNull Object getConfig() {
  Object response = configs.get(this);

  assert response != null;

  return response;
}

/**
 * @return Gets the config response for the selected enum wrapped with String.valueOf().
 */
public @NotNull String getStringConfig() {
  return String.valueOf(getConfig());
}

/**
 * @return Gets the config response for the selected enum wrapped with Integer.parseInt().
 */
public @NotNull Integer getIntegerConfig() {
  return Integer.parseInt(getStringConfig());
}


/**
 * @return Gets the config response for the selected enum wrapped with Double.parseDouble().
 */
public @NotNull Double getDoubleConfig() {
  return Double.parseDouble(getStringConfig());
}

public enum Occurrence {
  NEVER,
  FIRST,
  EVERY;
}

/**
 * @return Gets the config response for the selected enum wrapped with Occurrence.valueOf().
 */
public @NotNull Occurrence getOccurrenceConfig() {
  return Occurrence.valueOf(getStringConfig().toUpperCase());
}

/**
 Loads the default configs.
 */
public static void init() {
  //Loads the default values into the config.
  HashMap<String,Object> internalConfig = Util.parseInternalYaml("config.yml");
  internalConfig.forEach((String key, Object value) -> {
    String formattedKey = key.replace('.', '_');

    try {
      configs.put(Config.valueOf(formattedKey), value);
    } catch (IllegalArgumentException e) {
      //Dev warning
      throw new RuntimeException(formattedKey + " isn't in default config file.");
    }
  });

  //Checks if any default values are missing.
  for (Config config : Config.values()) {
    if (configs.containsKey(config)) continue;

    //Dev warning.
    throw new RuntimeException(config+" isn't in default config file.");
  }
}

/**
 Puts the keys response specified by the user into the configs map.
 */
public static void load() {
  //Loads in the user-set configs.
  File externalConfigFile = new File(Util.dataFolder.toPath()+File.separator+"config.yml");
  HashMap<String,Object> externalConfigs = Util.parseAndRepairExternalYaml(externalConfigFile, "config.yml");

  HashMap<Config, Object> userConfigs = new HashMap<>();

  //Gets the default keys that the user has entered.
  externalConfigs.forEach((String key, Object value) -> {
    String formattedKey = key.replace('.', '_');

    //logs an exception if the key doesn't exist.
    try {
      Config config = Config.valueOf(formattedKey);
      userConfigs.put(config, value);
    } catch (IllegalArgumentException e) {
      log.warning(Lang.excepts_invalidConfigKey.getResponse(Key.key.replaceWith(key)));
    }
  });


  //Warns the user about any config keys they are missing.
  for (Config config : configs.keySet()) {
    if (userConfigs.containsKey(config)) continue;

    String formattedKey = config.toString().replace('.', '_');
    log.warning(Lang.excepts_missingConfigKey.getResponse(Key.key.replaceWith(formattedKey)));
  }

  configs.putAll(userConfigs);
}

}
