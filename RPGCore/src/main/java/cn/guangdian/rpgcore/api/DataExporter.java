package cn.guangdian.rpgcore.api;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface DataExporter {

    CompletableFuture<File> exportToJson(String playerId, String dataType);

    CompletableFuture<File> exportAllToJson(String dataType);

    CompletableFuture<File> exportToCsv(String playerId, String dataType);

    CompletableFuture<File> exportAllToCsv(String dataType);

    CompletableFuture<File> exportAllPlayers(String dataType);

    List<String> getAvailableDataTypes();

    File getExportDirectory();

    interface ExportProgress {
        void onProgress(int current, int total, String status);
        void onComplete(File file);
        void onError(Exception e);
    }
}
