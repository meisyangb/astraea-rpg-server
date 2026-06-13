package cn.guangdian.board;

import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Arrays;
import java.util.List;

/**
 * 增量更新器 - 只更新变化的行
 *
 * <p>比较新旧内容，只更新发生变化的行，避免全量刷新。</p>
 *
 * <h3>性能优化：</h3>
 * <ul>
 *   <li>增量更新 - 只更新变化的行</li>
 *   <li>避免全量清空 - 减少Scoreboard API调用</li>
 *   <li>哈希快照 - 快速比较内容变化</li>
 * </ul>
 *
 * @author GuangDian
 * @since 2.0.0
 */
public class DifferentialUpdater {

    private static final int MAX_LINE_LENGTH = 40;
    private static final char[] SUFFIX_POOL = "abcdefghijklmnopqrstuvwxyz".toCharArray();

    /**
     * 更新状态
     */
    public static class UpdateResult {
        public final int totalLines;
        public final int updatedLines;
        public final int unchangedLines;

        UpdateResult(int total, int updated, int unchanged) {
            this.totalLines = total;
            this.updatedLines = updated;
            this.unchangedLines = unchanged;
        }

        public double getUpdateRatio() {
            return totalLines > 0 ? (double) updatedLines / totalLines : 0;
        }

        @Override
        public String toString() {
            return String.format("UpdateResult{total=%d, updated=%d, unchanged=%d, ratio=%.1f%%}",
                totalLines, updatedLines, unchangedLines, getUpdateRatio() * 100);
        }
    }

    /**
     * 行内容快照
     */
    public static class LineSnapshot {
        public final String content;
        public final int hash;
        public final int score;

        public LineSnapshot(String content, int score) {
            this.content = content;
            this.hash = content != null ? content.hashCode() : 0;
            this.score = score;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof LineSnapshot other)) return false;
            return this.hash == other.hash && this.score == other.score;
        }
    }

    /**
     * 增量更新 Scoreboard 内容
     *
     * <p>比较新旧内容，只更新变化的行。</p>
     *
     * @param board      Scoreboard 对象
     * @param objective  Objective 对象
     * @param oldSnapshot 旧行快照数组（可为null）
     * @param newLines   新的显示行列表
     * @param maxLines   最大行数
     * @return 更新结果
     */
    public static UpdateResult updateIncremental(Scoreboard board, Objective objective,
                                                  LineSnapshot[] oldSnapshot,
                                                  List<String> newLines, int maxLines) {
        int totalLines = Math.min(newLines.size(), maxLines);
        int updatedLines = 0;
        int unchangedLines = 0;

        if (oldSnapshot == null || oldSnapshot.length != totalLines) {
            // 快照不存在或长度不匹配，执行全量更新
            return updateFull(board, objective, newLines, maxLines);
        }

        // 计算新快照
        LineSnapshot[] newSnapshot = new LineSnapshot[totalLines];
        boolean[] needsUpdate = new boolean[totalLines];

        for (int i = 0; i < totalLines; i++) {
            String line = newLines.get(i);
            if (line == null || line.isEmpty()) {
                // 使用空字符串替代 ChatColor.RESET
                line = " ";
            }
            int score = totalLines - i;
            newSnapshot[i] = new LineSnapshot(line, score);

            // 比较快照
            if (!newSnapshot[i].equals(oldSnapshot[i])) {
                needsUpdate[i] = true;
            }
        }

        // 获取需要清除的旧条目
        for (String entry : board.getEntries()) {
            // 检查这个条目是否需要更新
            boolean found = false;
            for (int i = 0; i < totalLines; i++) {
                if (needsUpdate[i]) {
                    String expectedEntry = appendUniqueSuffix(newSnapshot[i].content, i);
                    if (entry.equals(expectedEntry)) {
                        found = true;
                        break;
                    }
                }
            }

            // 如果条目对应需要更新的行，清除它
            // 注意：这里采用简化策略，清除所有旧条目后重新设置
            // 更复杂的增量更新需要跟踪每个条目的位置
        }

        // 简化实现：清除所有需要更新的行的旧条目
        for (int i = 0; i < totalLines; i++) {
            if (needsUpdate[i]) {
                // 清除旧行对应的条目
                if (oldSnapshot[i] != null && oldSnapshot[i].content != null) {
                    String oldEntry = appendUniqueSuffix(oldSnapshot[i].content, i);
                    board.resetScores(oldEntry);
                }

                // 设置新行
                String entry = appendUniqueSuffix(newSnapshot[i].content, i);
                Score scoreObj = objective.getScore(entry);
                scoreObj.setScore(newSnapshot[i].score);
                updatedLines++;
            } else {
                unchangedLines++;
            }
        }

        return new UpdateResult(totalLines, updatedLines, unchangedLines);
    }

    /**
     * 全量更新 Scoreboard 内容
     */
    public static UpdateResult updateFull(Scoreboard board, Objective objective,
                                           List<String> lines, int maxLines) {
        int totalLines = Math.min(lines.size(), maxLines);

        // 清除所有旧条目
        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }

        // 设置新内容
        int score = totalLines;
        for (int i = 0; i < totalLines; i++) {
            String line = lines.get(i);
            if (line == null || line.isEmpty()) {
                // 使用空字符串替代 ChatColor.RESET
                line = " ";
            }

            String entry = appendUniqueSuffix(line, i);
            Score scoreObj = objective.getScore(entry);
            scoreObj.setScore(score--);
        }

        return new UpdateResult(totalLines, totalLines, 0);
    }

    /**
     * 创建行快照数组
     */
    public static LineSnapshot[] createSnapshot(List<String> lines, int maxLines) {
        int totalLines = Math.min(lines.size(), maxLines);
        LineSnapshot[] snapshot = new LineSnapshot[totalLines];

        for (int i = 0; i < totalLines; i++) {
            String line = lines.get(i);
            if (line == null || line.isEmpty()) {
                // 使用空字符串替代 ChatColor.RESET
                line = " ";
            }
            int score = totalLines - i;
            snapshot[i] = new LineSnapshot(line, score);
        }

        return snapshot;
    }

    /**
     * 快速比较两个快照数组
     */
    public static boolean snapshotsEqual(LineSnapshot[] a, LineSnapshot[] b) {
        if (a == null || b == null) return a == b;
        if (a.length != b.length) return false;
        return Arrays.equals(a, b);
    }

    /**
     * 附加唯一后缀
     */
    private static String appendUniqueSuffix(String line, int index) {
        char suffixChar = SUFFIX_POOL[index % SUFFIX_POOL.length];
        // 使用 § 字符替代 ChatColor.COLOR_CHAR
        return line + "§r§" + suffixChar;
    }
}