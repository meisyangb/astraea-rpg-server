package cn.guangdian.rpgcore.export;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.DataExporter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class DataExporterImpl implements DataExporter {

    private final RPGCore plugin;
    private final Gson gson;
    private final File exportDir;
    private final SimpleDateFormat dateFormat;
    private final Set<String> registeredDataTypes = new HashSet<>();
    private final AsyncScheduler asyncScheduler;

    public DataExporterImpl(RPGCore plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.exportDir = new File(plugin.getDataFolder(), "exports");
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        this.asyncScheduler = Bukkit.getAsyncScheduler();

        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        plugin.getLogger().info("[DataExporter] Initialized, export dir: " + exportDir.getAbsolutePath());
    }

    public void registerDataType(String dataType) {
        registeredDataTypes.add(dataType);
        plugin.getLogger().info("[DataExporter] Registered data type: " + dataType);
    }

    @Override
    public CompletableFuture<File> exportToJson(String playerId, String dataType) {
        return CompletableFuture.supplyAsync(() -> {
            File file = createExportFile(playerId, dataType, "json");

            try {
                JsonObject root = new JsonObject();
                root.addProperty("playerId", playerId);
                root.addProperty("dataType", dataType);
                root.addProperty("exportTime", new Date().toString());

                JsonObject data = fetchPlayerData(playerId, dataType);
                root.add("data", data);

                try (FileWriter writer = new FileWriter(file)) {
                    gson.toJson(root, writer);
                }

                plugin.getLogger().info("[DataExporter] Exported " + playerId + "/" + dataType + " to JSON");
                return file;

            } catch (IOException e) {
                plugin.getLogger().severe("[DataExporter] Export failed: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, runnable -> asyncScheduler.runNow(plugin, scheduledTask -> runnable.run()));
    }

    @Override
    public CompletableFuture<File> exportAllToJson(String dataType) {
        return CompletableFuture.supplyAsync(() -> {
            File file = createExportFile("all_" + dataType, dataType, "json");

            try {
                JsonObject root = new JsonObject();
                root.addProperty("dataType", dataType);
                root.addProperty("exportTime", new Date().toString());

                JsonArray players = new JsonArray();
                for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                    JsonObject playerData = new JsonObject();
                    playerData.addProperty("playerId", player.getUniqueId().toString());
                    playerData.addProperty("playerName", player.getName());
                    playerData.add("data", fetchPlayerData(player.getUniqueId().toString(), dataType));
                    players.add(playerData);
                }
                root.add("players", players);

                try (FileWriter writer = new FileWriter(file)) {
                    gson.toJson(root, writer);
                }

                plugin.getLogger().info("[DataExporter] Exported all players/" + dataType + " to JSON");
                return file;

            } catch (IOException e) {
                plugin.getLogger().severe("[DataExporter] Export failed: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, runnable -> asyncScheduler.runNow(plugin, scheduledTask -> runnable.run()));
    }

    @Override
    public CompletableFuture<File> exportToCsv(String playerId, String dataType) {
        return CompletableFuture.supplyAsync(() -> {
            File file = createExportFile(playerId, dataType, "csv");

            try {
                JsonObject data = fetchPlayerData(playerId, dataType);

                StringBuilder csv = new StringBuilder();
                csv.append("key,value\n");
                data.entrySet().forEach(entry -> {
                    csv.append(escapeCsv(entry.getKey())).append(",");
                    csv.append(escapeCsv(entry.getValue().toString())).append("\n");
                });

                Files.writeString(file.toPath(), csv.toString());

                plugin.getLogger().info("[DataExporter] Exported " + playerId + "/" + dataType + " to CSV");
                return file;

            } catch (IOException e) {
                plugin.getLogger().severe("[DataExporter] CSV export failed: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, runnable -> asyncScheduler.runNow(plugin, scheduledTask -> runnable.run()));
    }

    @Override
    public CompletableFuture<File> exportAllToCsv(String dataType) {
        return CompletableFuture.supplyAsync(() -> {
            File file = createExportFile("all_" + dataType, dataType, "csv");

            try {
                StringBuilder csv = new StringBuilder();
                csv.append("playerId,playerName,");

                List<String> keys = new ArrayList<>();
                for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                    JsonObject data = fetchPlayerData(player.getUniqueId().toString(), dataType);
                    if (keys.isEmpty()) {
                        data.keySet().forEach(keys::add);
                        keys.forEach(k -> csv.append(escapeCsv(k)).append(","));
                        csv.append("\n");
                    }
                    csv.append(player.getUniqueId()).append(",");
                    csv.append(escapeCsv(player.getName())).append(",");
                    keys.forEach(k -> {
                        if (data.has(k)) {
                            csv.append(escapeCsv(data.get(k).toString())).append(",");
                        } else {
                            csv.append(",");
                        }
                    });
                    csv.append("\n");
                }

                Files.writeString(file.toPath(), csv.toString());

                plugin.getLogger().info("[DataExporter] Exported all players/" + dataType + " to CSV");
                return file;

            } catch (IOException e) {
                plugin.getLogger().severe("[DataExporter] CSV export failed: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, runnable -> asyncScheduler.runNow(plugin, scheduledTask -> runnable.run()));
    }

    @Override
    public CompletableFuture<File> exportAllPlayers(String dataType) {
        return exportAllToJson(dataType);
    }

    @Override
    public List<String> getAvailableDataTypes() {
        return new ArrayList<>(registeredDataTypes);
    }

    @Override
    public File getExportDirectory() {
        return exportDir;
    }

    private File createExportFile(String prefix, String dataType, String extension) {
        String timestamp = dateFormat.format(new Date());
        String filename = String.format("%s_%s_%s.%s", prefix, dataType, timestamp, extension);
        return new File(exportDir, filename);
    }

    private JsonObject fetchPlayerData(String playerId, String dataType) {
        JsonObject data = new JsonObject();
        data.addProperty("fetched", "placeholder");
        data.addProperty("note", "Implement data fetching for " + dataType);
        return data;
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}