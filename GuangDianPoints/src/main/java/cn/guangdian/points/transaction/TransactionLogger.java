package cn.guangdian.points.transaction;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

/**
 * 事务日志记录器
 * 记录所有点券操作，支持崩溃恢复
 */
public class TransactionLogger {

    private final JavaPlugin plugin;
    private final File logFile;
    private final File logDirectory;
    private BufferedWriter writer;
    private final AtomicLong transactionIdGenerator = new AtomicLong(0);
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    // 待提交的事务
    private final Map<String, TransactionRecord> pendingTransactions = new HashMap<>();

    /**
     * 事务类型枚举
     */
    public enum TransactionType {
        EARN,           // 获得点券
        SPEND,          // 消费点券
        TRANSFER_OUT,   // 转账发出
        TRANSFER_IN,    // 转账接收
        ADMIN_GIVE,     // 管理员给予
        ADMIN_TAKE,     // 管理员扣除
        ADMIN_SET       // 管理员设置
    }

    /**
     * 事务状态枚举
     */
    public enum TransactionStatus {
        PENDING,        // 待处理
        COMMITTED,      // 已提交
        ROLLED_BACK     // 已回滚
    }

    /**
     * 创建事务日志记录器
     */
    public TransactionLogger(JavaPlugin plugin, File logFile) {
        this.plugin = plugin;
        this.logFile = logFile;
        this.logDirectory = logFile.getParentFile();

        initializeLogger();
    }

    /**
     * 初始化日志记录器
     */
    private void initializeLogger() {
        try {
            if (!logDirectory.exists()) {
                if (!logDirectory.mkdirs()) {
                    throw new IOException("无法创建日志目录: " + logDirectory.getAbsolutePath());
                }
            }

            if (!logFile.exists()) {
                if (!logFile.createNewFile()) {
                    throw new IOException("无法创建日志文件: " + logFile.getAbsolutePath());
                }
            }

            // 追加模式打开文件 - 使用 try-with-resources 测试文件可写性
            try (OutputStreamWriter testWriter = new OutputStreamWriter(
                    new FileOutputStream(logFile, true), StandardCharsets.UTF_8)) {
                // 测试成功，创建实际的 writer
            }
            
            // 创建实际的 writer
            writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(logFile, true), StandardCharsets.UTF_8));

            // 加载事务ID生成器的初始值
            loadLastTransactionId();

            plugin.getLogger().info("事务日志记录器初始化成功: " + logFile.getName());

        } catch (IOException e) {
            // 确保失败时清理资源
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException closeEx) {
                    // 忽略关闭异常
                }
                writer = null;
            }
            plugin.getLogger().log(Level.SEVERE, "初始化事务日志记录器失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从日志文件加载最后一个事务ID
     */
    private void loadLastTransactionId() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(logFile), StandardCharsets.UTF_8))) {

            String line;
            long maxId = 0;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("TXN-")) {
                    try {
                        String[] parts = line.split("\\|");
                        if (parts.length > 0) {
                            String idStr = parts[0].substring(4); // 去掉 "TXN-"
                            long id = Long.parseLong(idStr);
                            maxId = Math.max(maxId, id);
                        }
                    } catch (NumberFormatException ignored) {
                        // 忽略解析错误
                    }
                }
            }

            transactionIdGenerator.set(maxId);
            plugin.getLogger().info("事务ID生成器初始化为: " + maxId);

        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "加载事务ID失败，从0开始", e);
        }
    }

    /**
     * 开始事务，返回唯一事务ID
     */
    public String beginTransaction(UUID playerUuid, TransactionType type, long amount) {
        return beginTransaction(playerUuid, type, amount, -1, null);
    }

    /**
     * 开始事务（带关联玩家）
     */
    public String beginTransaction(UUID playerUuid, TransactionType type, long amount,
                                   long relatedPlayerUuid, String reason) {
        String transactionId = generateTransactionId();
        long timestamp = System.currentTimeMillis();
        long balanceBefore = -1; // 需要在调用时设置

        TransactionRecord record = new TransactionRecord(
            transactionId, playerUuid, type, amount,
            balanceBefore, -1, relatedPlayerUuid, reason,
            timestamp, TransactionStatus.PENDING
        );

        lock.writeLock().lock();
        try {
            pendingTransactions.put(transactionId, record);
            writeLog("BEGIN", record);
        } finally {
            lock.writeLock().unlock();
        }

        return transactionId;
    }

    /**
     * 记录事务开始（带完整信息）
     */
    public String beginTransaction(UUID playerUuid, TransactionType type, long amount,
                                   long balanceBefore, long balanceAfter,
                                   UUID relatedPlayer, String reason) {
        String transactionId = generateTransactionId();
        long timestamp = System.currentTimeMillis();

        TransactionRecord record = new TransactionRecord(
            transactionId, playerUuid, type, amount,
            balanceBefore, balanceAfter,
            relatedPlayer != null ? relatedPlayer.getMostSignificantBits() : -1,
            reason, timestamp, TransactionStatus.PENDING
        );

        lock.writeLock().lock();
        try {
            pendingTransactions.put(transactionId, record);
            writeLog("BEGIN", record);
        } finally {
            lock.writeLock().unlock();
        }

        return transactionId;
    }

    /**
     * 提交事务
     */
    public void commitTransaction(String transactionId) {
        commitTransaction(transactionId, -1);
    }

    /**
     * 提交事务（带余额信息）
     */
    public void commitTransaction(String transactionId, long balanceAfter) {
        lock.writeLock().lock();
        try {
            TransactionRecord record = pendingTransactions.remove(transactionId);
            if (record != null) {
                record.status = TransactionStatus.COMMITTED;
                record.balanceAfter = balanceAfter;
                writeLog("COMMIT", record);
            } else {
                // 即使不在pending中，也记录提交
                writeCommitLog(transactionId);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 回滚事务
     */
    public void rollbackTransaction(String transactionId, String reason) {
        lock.writeLock().lock();
        try {
            TransactionRecord record = pendingTransactions.remove(transactionId);
            if (record != null) {
                record.status = TransactionStatus.ROLLED_BACK;
                writeLog("ROLLBACK", record, reason);
            } else {
                // 即使不在pending中，也记录回滚
                writeRollbackLog(transactionId, reason);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 从日志恢复未完成的事务
     */
    public List<UnfinishedTransaction> recoverUnfinishedTransactions() {
        List<UnfinishedTransaction> unfinished = new ArrayList<>();
        Map<String, TransactionRecord> allTransactions = new HashMap<>();
        Set<String> completedTransactions = new HashSet<>();

        lock.readLock().lock();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(logFile), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    TransactionLogEntry entry = parseLogLine(line);
                    if (entry != null) {
                        if (entry.action.equals("BEGIN")) {
                            allTransactions.put(entry.transactionId, entry.record);
                        } else if (entry.action.equals("COMMIT") || entry.action.equals("ROLLBACK")) {
                            completedTransactions.add(entry.transactionId);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "解析日志行失败: " + line, e);
                }
            }

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "读取事务日志失败", e);
        } finally {
            lock.readLock().unlock();
        }

        // 找出未完成的事务
        for (Map.Entry<String, TransactionRecord> entry : allTransactions.entrySet()) {
            if (!completedTransactions.contains(entry.getKey())) {
                TransactionRecord record = entry.getValue();
                unfinished.add(new UnfinishedTransaction(
                    record.transactionId,
                    record.playerUuid,
                    record.type,
                    record.amount,
                    record.timestamp,
                    record.balanceBefore
                ));
            }
        }

        if (!unfinished.isEmpty()) {
            plugin.getLogger().warning("发现 " + unfinished.size() + " 个未完成的事务需要恢复");
        }

        return unfinished;
    }

    /**
     * 清理旧日志
     */
    public void cleanupOldLogs(long retentionDays) {
        File[] logFiles = logDirectory.listFiles((dir, name) ->
            name.startsWith("transactions") && name.endsWith(".log"));

        if (logFiles == null) return;

        long cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L);

        for (File file : logFiles) {
            if (file.lastModified() < cutoffTime && !file.equals(logFile)) {
                if (file.delete()) {
                    plugin.getLogger().info("已清理旧日志文件: " + file.getName());
                }
            }
        }
    }

    /**
     * 归档当前日志文件
     */
    public void archiveCurrentLog() {
        lock.writeLock().lock();
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
            }

            // 重命名当前日志文件
            String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            File archivedFile = new File(logDirectory, "transactions-" + timestamp + ".log");
            if (logFile.renameTo(archivedFile)) {
                plugin.getLogger().info("已归档日志文件: " + archivedFile.getName());

                // 创建新的日志文件
                logFile.createNewFile();
                writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(logFile, true), StandardCharsets.UTF_8));
            }

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "归档日志文件失败", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 关闭日志记录器
     */
    public void close() {
        lock.writeLock().lock();
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
                writer = null;
            }
            plugin.getLogger().info("事务日志记录器已关闭");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "关闭事务日志记录器失败", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 生成唯一事务ID
     */
    private String generateTransactionId() {
        long id = transactionIdGenerator.incrementAndGet();
        return "TXN-" + id;
    }

    /**
     * 写入日志
     */
    private void writeLog(String action, TransactionRecord record) {
        writeLog(action, record, null);
    }

    /**
     * 写入日志（带额外信息）
     */
    private void writeLog(String action, TransactionRecord record, String extra) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(record.transactionId);
            sb.append("|").append(action);
            sb.append("|").append(dateFormat.format(new Date(record.timestamp)));
            sb.append("|").append(record.playerUuid);
            sb.append("|").append(record.type.name());
            sb.append("|").append(record.amount);
            sb.append("|").append(record.balanceBefore);
            if (record.balanceAfter >= 0) {
                sb.append("|").append(record.balanceAfter);
            }
            if (record.relatedPlayer > 0) {
                sb.append("|related:").append(new UUID(record.relatedPlayer, 0));
            }
            if (record.reason != null) {
                sb.append("|reason:").append(record.reason);
            }
            if (extra != null) {
                sb.append("|").append(extra);
            }

            writer.write(sb.toString());
            writer.newLine();
            writer.flush();

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "写入事务日志失败", e);
        }
    }

    /**
     * 写入提交日志
     */
    private void writeCommitLog(String transactionId) {
        try {
            writer.write(transactionId + "|COMMIT|" + dateFormat.format(new Date()));
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "写入提交日志失败", e);
        }
    }

    /**
     * 写入回滚日志
     */
    private void writeRollbackLog(String transactionId, String reason) {
        try {
            writer.write(transactionId + "|ROLLBACK|" + dateFormat.format(new Date()) + "|reason:" + reason);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "写入回滚日志失败", e);
        }
    }

    /**
     * 解析日志行
     */
    private TransactionLogEntry parseLogLine(String line) {
        if (line == null || line.isEmpty()) return null;

        String[] parts = line.split("\\|");
        if (parts.length < 2) return null;

        String transactionId = parts[0];
        String action = parts[1];

        if (action.equals("BEGIN") && parts.length >= 6) {
            try {
                TransactionRecord record = new TransactionRecord();
                record.transactionId = transactionId;
                record.playerUuid = UUID.fromString(parts[3]);
                record.type = TransactionType.valueOf(parts[4]);
                record.amount = Long.parseLong(parts[5]);
                if (parts.length > 6) {
                    record.balanceBefore = Long.parseLong(parts[6]);
                }
                record.timestamp = System.currentTimeMillis();
                record.status = TransactionStatus.PENDING;

                return new TransactionLogEntry(action, transactionId, record);
            } catch (Exception e) {
                return null;
            }
        }

        return new TransactionLogEntry(action, transactionId, null);
    }

    /**
     * 事务记录内部类
     */
    private static class TransactionRecord {
        String transactionId;
        UUID playerUuid;
        TransactionType type;
        long amount;
        long balanceBefore;
        long balanceAfter;
        long relatedPlayer;
        String reason;
        long timestamp;
        TransactionStatus status;

        TransactionRecord() {}

        TransactionRecord(String transactionId, UUID playerUuid, TransactionType type,
                         long amount, long balanceBefore, long balanceAfter,
                         long relatedPlayer, String reason, long timestamp,
                         TransactionStatus status) {
            this.transactionId = transactionId;
            this.playerUuid = playerUuid;
            this.type = type;
            this.amount = amount;
            this.balanceBefore = balanceBefore;
            this.balanceAfter = balanceAfter;
            this.relatedPlayer = relatedPlayer;
            this.reason = reason;
            this.timestamp = timestamp;
            this.status = status;
        }
    }

    /**
     * 日志条目内部类
     */
    private static class TransactionLogEntry {
        String action;
        String transactionId;
        TransactionRecord record;

        TransactionLogEntry(String action, String transactionId, TransactionRecord record) {
            this.action = action;
            this.transactionId = transactionId;
            this.record = record;
        }
    }

    /**
     * 获取当前事务ID计数
     */
    public long getCurrentTransactionId() {
        return transactionIdGenerator.get();
    }

    /**
     * 获取待处理事务数量
     */
    public int getPendingTransactionCount() {
        lock.readLock().lock();
        try {
            return pendingTransactions.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}