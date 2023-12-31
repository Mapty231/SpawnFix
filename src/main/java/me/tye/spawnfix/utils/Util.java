package me.tye.spawnfix.utils;

import me.tye.spawnfix.SpawnFix;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Util {

/**
 This plugin.
 */
public static final JavaPlugin plugin = JavaPlugin.getPlugin(SpawnFix.class);


/**
 The data folder.
 */
public static final File dataFolder = plugin.getDataFolder();

/**
 The config file for this plugin.
 */
public static final File configFile = new File(dataFolder.toPath() + File.separator + "config.yml");

/**
 The lang folder for this plugin.
 */
public static File langFolder = new File(dataFolder.toPath() + File.separator + "langFiles");

/**
 The logger for this plugin.
 */
public static final Logger log = plugin.getLogger();


/**
 * @return The default spawn location as set in the config.yml of this plugin.
 */
public static Location getDefaultSpawn() {
  return new Location(Bukkit.getWorld(Config.default_worldName.getStringConfig()),
      Config.default_x.getDoubleConfig(),
      Config.default_y.getDoubleConfig(),
      Config.default_z.getDoubleConfig(),
      Config.default_yaw.getFloatConfig(),
      Config.default_pitch.getFloatConfig()
  );
}


/**
 Formats the Map returned from Yaml.load() into a hashmap where the exact key corresponds to the value.<br>
 E.G: key: "example.response" value: "test".
 @param baseMap The Map from Yaml.load().
 @return The formatted Map. */
public static @NotNull HashMap<String,Object> getKeysRecursive(@Nullable Map<?,?> baseMap) {
  HashMap<String,Object> map = new HashMap<>();
  if (baseMap == null) return map;

  for (Object key : baseMap.keySet()) {
    Object value = baseMap.get(key);

    if (value instanceof Map<?,?> subMap) {
      map.putAll(getKeysRecursive(String.valueOf(key), subMap));
    } else {
      map.put(String.valueOf(key), String.valueOf(value));
    }

  }

  return map;
}

/**
 Formats the Map returned from Yaml.load() into a hashmap where the exact key corresponds to the value.
 @param keyPath The path to append to the starts of the key. (Should only be called internally).
 @param baseMap The Map from Yaml.load().
 @return The formatted Map. */
public static @NotNull HashMap<String,Object> getKeysRecursive(@NotNull String keyPath, @NotNull Map<?,?> baseMap) {
  if (!keyPath.isEmpty()) keyPath += ".";

  HashMap<String,Object> map = new HashMap<>();
  for (Object key : baseMap.keySet()) {
    Object value = baseMap.get(key);

    if (value instanceof Map<?,?> subMap) {
      map.putAll(getKeysRecursive(keyPath+key, subMap));
    } else {
      map.put(keyPath+key, String.valueOf(value));
    }

  }

  return map;
}

/**
 Copies the content of an internal file to a new external one.
 @param file     External file destination
 @param resource Input stream for the data to write, or null if target is an empty file/dir.
 @param isFile Set to true to create a file. Set to false to create a dir.*/
public static void makeRequiredFile(@NotNull File file, @Nullable InputStream resource, boolean isFile) throws IOException {
  if (file.exists())
    return;

  if (isFile) {
    if (!file.createNewFile())
      throw new IOException();
  }
  else {
    if (!file.mkdir())
      throw new IOException();
  }

  if (resource != null) {
    String text = new String(resource.readAllBytes());
    FileWriter fw = new FileWriter(file);
    fw.write(text);
    fw.close();
  }
}

/**
 Copies the content of an internal file to a new external one.
 @param file     External file destination
 @param resource Input stream for the data to write, or null if target is an empty file/dir.
 @param isFile Set to true to create a file. Set to false to create a dir.*/
public static void createFile(@NotNull File file, @Nullable InputStream resource, boolean isFile) {
  try {
    makeRequiredFile(file, resource, isFile);
  } catch (IOException e) {
    log.log(Level.WARNING, Lang.excepts_fileCreation.getResponse(Key.filePath.replaceWith(file.getAbsolutePath())), e);
  }
}


/**
 Parses & formats data from the given inputStream to a Yaml resource.
 * @param yamlInputStream The given inputStream to a Yaml resource.
 * @return The parsed values in the format key: "test1.log" value: "works!"<br>
 * Or an empty hashMap if the given inputStream is null.
 * @throws IOException If the data couldn't be read from the given inputStream.
 */
private static @NotNull HashMap<String, Object> parseYaml(@Nullable InputStream yamlInputStream) throws IOException {
  if (yamlInputStream == null) return new HashMap<>();

  byte[] resourceBytes = yamlInputStream.readAllBytes();

  String resourceContent = new String(resourceBytes, Charset.defaultCharset());

  return getKeysRecursive(new Yaml().load(resourceContent));
}

/**
 Parses the data from an internal YAML file.
 * @param resourcePath The path to the file from /src/main/resource/
 * @return The parsed values in the format key: "test1.log" value: "works!" <br>
 * Or an empty hashMap if the file couldn't be found or read.
 */
public static @NotNull HashMap<String, Object> parseInternalYaml(@NotNull String resourcePath) {
  try (InputStream resourceInputStream = plugin.getResource(resourcePath)) {
    return parseYaml(resourceInputStream);

  } catch (IOException e) {
    log.log(Level.SEVERE, "Unable to parse internal YAML files.\nConfig & lang might break.\n", e);
    return new HashMap<>();
  }

}


/**
 Parses the given external file into a hashMap. If the internal file contained keys that the external file didn't then the key-value pare is added to the external file.
 * @param externalFile The external file to parse.
 * @param pathToInternalResource The path to the internal resource to repair it with or fallback on if the external file is broken.
 * @return The key-value pairs from the external file. If any keys were missing from the external file then they are put into the hashMap with their default value.
 */
public static @NotNull HashMap<String, Object> parseAndRepairExternalYaml(@NotNull File externalFile, @Nullable String pathToInternalResource) {
  HashMap<String,Object> externalYaml;

  //tries to parse the external file.
  try (InputStream externalInputStream = new FileInputStream(externalFile)) {
    externalYaml = parseYaml(externalInputStream);

  } catch (FileNotFoundException e) {
    log.log(Level.SEVERE, Lang.excepts_noFile.getResponse(Key.filePath.replaceWith(externalFile.getAbsolutePath())), e);

    //returns an empty hashMap or the internal values if present.
    return pathToInternalResource == null ? new HashMap<>() : parseInternalYaml(pathToInternalResource);

  } catch (IOException e) {
    log.log(Level.SEVERE, Lang.excepts_parseYaml.getResponse(Key.filePath.replaceWith(externalFile.getAbsolutePath())), e);

    //returns an empty hashMap or the internal values if present.
    return pathToInternalResource == null ? new HashMap<>() : parseInternalYaml(pathToInternalResource);
  }


  //if there is no internal resource to compare against then only the external file data is returned.
  if (pathToInternalResource == null)
    return externalYaml;

  HashMap<String,Object> internalYaml = parseInternalYaml(pathToInternalResource);

  //gets the values that the external file is missing;
  HashMap<String,Object> missingPairsMap = new HashMap<>();
  internalYaml.forEach((String key, Object value) -> {
    if (externalYaml.containsKey(key))
      return;

    missingPairsMap.put(key, value);
  });

  //if no values are missing return
  if (missingPairsMap.keySet().isEmpty())
    return externalYaml;

  //Adds all the missing key-value pairs to a stringBuilder.
  StringBuilder missingPairs = new StringBuilder("\n");
  missingPairsMap.forEach((String key, Object value) -> {
    missingPairs.append(key)
                .append(": \"")
                .append(preserveEscapedQuotes(value))
                .append("\"\n");
  });

  //Adds al the missing pairs to the external Yaml.
  externalYaml.putAll(missingPairsMap);


  //Writes the missing pairs to the external file.
  try (FileWriter externalFileWriter = new FileWriter(externalFile, true)) {
    externalFileWriter.append(missingPairs.toString());

  }catch (IOException e) {
    //Logs a warning
    log.log(Level.WARNING, Lang.excepts_fileRestore.getResponse(Key.filePath.replaceWith(externalFile.getAbsolutePath())), e);

    //Logs the keys that couldn't be appended.
    missingPairsMap.forEach((String key, Object value) -> {
      log.log(Level.WARNING, key + ": " + value);
    });
  }

  return externalYaml;
}

/**
 Object.toString() changes \" to ". This method resolves this problem.
 * @param value The object to get the string from.
 * @return The correct string from the given object.
 */
private static String preserveEscapedQuotes(Object value) {
  char[] valueCharArray = value.toString().toCharArray();
  StringBuilder correctString = new StringBuilder();


  for (char character : valueCharArray) {
    if (character != '"') {
      correctString.append(character);
      continue;
    }

    correctString.append('\\');
    correctString.append('"');
  }

  return correctString.toString();
}

/**
 Writes the new value to the key in the specified external yaml file.
 * @param key The key to replace the value of.
 * @param value The new string to overwrite the old value with.
 * @param externalYaml The external yaml to perform this operation on.
 * @throws IOException If there was an error reading or writing data to the Yaml file.
 */
public static void writeYamlData(@NotNull String key, @NotNull String value, @NotNull File externalYaml) throws IOException {
  String fileContent;

  //Reads the content from the file.
  try (FileInputStream externalYamlInputStream = new FileInputStream(externalYaml)) {
    fileContent = new String(externalYamlInputStream.readAllBytes());
  }

  //Ensures that every key ends with ":".
  //This avoids false matches where one key contains the starting chars of another.
  String[] keys = key.split("\\.");
  for (int i = 0; i < keys.length; i++) {
    String k = keys[i];

    if (k.endsWith(":")) continue;

    k+=":";
    keys[i] = k;
  }


  Integer valueStartPosition = findKeyPosition(keys, fileContent);
  if (valueStartPosition == null) throw new IOException("Unable to find "+key+" in external Yaml file.");

  int valueLineEnd = valueStartPosition;
  //Finds the index of the end of the line that the key is on.
  while (fileContent.charAt(valueLineEnd) != '\n') {
    valueLineEnd++;
  }

  String firstPart = fileContent.substring(0, valueStartPosition);
  String secondPart = fileContent.substring(valueLineEnd, fileContent.length()-1);

  String newFileContent = firstPart.concat(value).concat(secondPart);

  try (FileWriter externalYamlWriter = new FileWriter(externalYaml)) {
    externalYamlWriter.write(newFileContent);
  }
}

/**
 Finds the given key index in the given file content split on each new line.<br>
 The key should be given in the format "example.key1".
 * @param keys The given keys.
 * @param fileContent The given file content.
 * @return The index of the char a space after the end of the last key.<br>
 * Example: "key1:  !" the char index that the '!' is on.<br>
 * Or null if the key couldn't be found.
 */
private static @Nullable Integer findKeyPosition(@NotNull String[] keys, @NotNull String fileContent) {

  //Iterates over each line in the file content.
  String[] split = fileContent.split("\n");

  for (int keyLine = 0, splitLength = split.length; keyLine < splitLength; keyLine++) {
    String strippedLine = split[keyLine].stripLeading();

    //if it doesn't start with the key value then continue
    if (!strippedLine.startsWith(keys[0])) {
      continue;
    }

    return findKeyPosition(keys, keyLine+1, 1, fileContent);

  }

  return null;
}

/**
 Gets the index for the line of the last sub-key given in the given fileContent.<br>
 This method executes recursively.
 * @param keys The keys to find in the file.
 * @param startLine The index of the line to start the search from.
 * @param keyIndex The index of the sub-key that is searched for.
 * @param fileContent The content of the file to search through.
 * @return The index of the char a space after the end of the last key.<br>
 * Example: "key1:  !" the char index that the '!' is on.<br>
 * Or null if the key couldn't be found.
 */
private static @Nullable Integer findKeyPosition(@NotNull String[] keys, int startLine, int keyIndex, @NotNull String fileContent) {
  //Iterates over each line in the file content.
  String[] split = fileContent.split("\n");

  for (int lineIndex = startLine, splitLength = split.length; lineIndex < splitLength; lineIndex++) {

    String line = split[lineIndex];

    //returns null if the key doesn't exist as a sub-key of the base key.
    if (!(line.startsWith(" ") || line.startsWith("\t"))) {
      return null;
    }

    String strippedLine = line.stripLeading();

    //if it doesn't start with the key value then continue
    if (!strippedLine.startsWith(keys[keyIndex])) {
      continue;
    }

    keyIndex++;

    //returns the char position that the key ends on if it is the last key in the sequence.
    if (keyIndex == keys.length) {

      int currentLinePosition = 0;
      //Gets the char position of the current line within the string.
      for (int i = 0; i < lineIndex; i++) {
        currentLinePosition+=split[i].length()+1;
      }

      //Finds the char position a space after the key ends.
      for (int i = currentLinePosition; i < fileContent.length(); i++) {
        char character = fileContent.charAt(i);

        if (character != ':') continue;

        return i+2;

      }
    }

    return findKeyPosition(keys, startLine, keyIndex, fileContent);
  }

  return null;
}

}
