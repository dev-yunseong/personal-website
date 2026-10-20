package dev.yunseong.website.blog.service;

import java.util.ArrayList;
import java.util.List;

/** Line-oriented review, with bounded memory for unusually large documents. */
public final class MemoLineDiff {
    private MemoLineDiff() {}

    public static List<Line> compare(String before, String after) {
        String[] oldLines = lines(before);
        String[] newLines = lines(after);
        List<Line> result = new ArrayList<>();
        if ((long) (oldLines.length + 1) * (newLines.length + 1) > 1_000_000) {
            for (int i = 0; i < oldLines.length; i++) result.add(new Line("removed", "-", i + 1, null, oldLines[i]));
            for (int j = 0; j < newLines.length; j++) result.add(new Line("added", "+", null, j + 1, newLines[j]));
            return result;
        }
        int[][] lengths = new int[oldLines.length + 1][newLines.length + 1];
        for (int i = oldLines.length - 1; i >= 0; i--) {
            for (int j = newLines.length - 1; j >= 0; j--) {
                lengths[i][j] = oldLines[i].equals(newLines[j]) ? 1 + lengths[i + 1][j + 1]
                        : Math.max(lengths[i + 1][j], lengths[i][j + 1]);
            }
        }
        int i = 0;
        int j = 0;
        while (i < oldLines.length || j < newLines.length) {
            if (i < oldLines.length && j < newLines.length && oldLines[i].equals(newLines[j])) {
                result.add(new Line("context", " ", i + 1, j + 1, oldLines[i]));
                i++;
                j++;
            } else if (i < oldLines.length && (j == newLines.length || lengths[i + 1][j] >= lengths[i][j + 1])) {
                result.add(new Line("removed", "-", i + 1, null, oldLines[i++]));
            } else {
                result.add(new Line("added", "+", null, j + 1, newLines[j++]));
            }
        }
        return result;
    }

    private static String[] lines(String content) {
        return content == null || content.isEmpty() ? new String[0] : content.split("\n", -1);
    }

    public record Line(String kind, String marker, Integer oldNumber, Integer newNumber, String text) {}
}
