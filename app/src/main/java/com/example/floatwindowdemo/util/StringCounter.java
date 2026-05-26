package com.example.floatwindowdemo.util;

public class StringCounter {
//    public static int countLinesStartingAndContaining(String input) {
//        if (input == null || input.trim().isEmpty()) {
//            return 0;
//        }
//
//        String[] lines = input.split("\\r?\\n"); // 兼容 Windows (\r\n) 和 Unix (\n)
//        int count = 0;
//
//        for (String line : lines) {
//            line = line.trim();
//            if (line.startsWith("fl") && line.contains("SECURE")) {
//                count++;
//            }
//        }
//
//        return count;
//    }

    public static int countLinesStartingAndContaining(CharSequence input) {
        if (input == null || input.length() == 0) {
            return 0;
        }

        int count = 0;
        int i = 0;
        final int len = input.length();
        int skipNextLine = 0; // 用于标记是否跳过下一行

        while (i < len) {
            // 跳过多余的换行符(\r\n或\n)
            while (i < len && (input.charAt(i) == '\n' || input.charAt(i) == '\r')) {
                i++;
            }
            if (i >= len) break;

            int lineStart = i;

            // 找到行尾(后续的i表示行尾索引)
            while (i < len && input.charAt(i) != '\n' && input.charAt(i) != '\r') {
                i++;
            }

            // 如果需要跳过这一行
            if (skipNextLine > 0) {
                skipNextLine--; // 重置标志
                continue; // 跳过处理
            }

            // 跳过行首空白
            int start = lineStart;
            while (start < i && Character.isWhitespace(input.charAt(start))) {
                start++;
            }

            // 检查是否以"mAtt"开头
            if (start + 4 <= i &&
                    input.charAt(start) == 'm' &&
                    input.charAt(start + 1) == 'O' &&
                    input.charAt(start + 2) == 'w' &&
                    input.charAt(start + 3) == 'n') {
                // 检查是否包含"com.example.floatwindowdemo"
                if (containsPackageName(input, start, i)){
                    skipNextLine = 3; // 设置标志：下3行跳过
                    continue; // 不再检查 fl/SECURE 条件
                }
            }

            // 检查是否以"fl"开头
            if (start + 2 <= i &&
                    input.charAt(start) == 'f' &&
                    input.charAt(start + 1) == 'l') {
                // 检查是否包含"SECURE"（不创建子字符串）
                if (containsSecure(input, start, i)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static boolean containsSecure(CharSequence cs, int start, int end) {
        // 高效的子串搜索（比String.contains()更快且不创建对象）
        final int targetLen = 6; // "SECURE".length()
        for (int i = start; i <= end - targetLen; i++) {
            if (cs.charAt(i) == 'S' &&
                    cs.charAt(i + 1) == 'E' &&
                    cs.charAt(i + 2) == 'C' &&
                    cs.charAt(i + 3) == 'U' &&
                    cs.charAt(i + 4) == 'R' &&
                    cs.charAt(i + 5) == 'E') {
                return true;
            }
        }
        return false;
    }

    private static boolean containsPackageName(CharSequence cs, int start, int end) {
        // 高效的子串搜索（比String.contains()更快且不创建对象）
        final int targetLen = 27;
        for (int i = start; i <= end - targetLen; i++) {
            if (cs.charAt(i) == 'c' &&
                    cs.charAt(i + 1) == 'o' &&
                    cs.charAt(i + 2) == 'm' &&
                    cs.charAt(i + 3) == '.' &&
                    cs.charAt(i + 4) == 'e' &&
                    cs.charAt(i + 5) == 'x' &&
                    cs.charAt(i + 6) == 'a' &&
                    cs.charAt(i + 7) == 'm' &&
                    cs.charAt(i + 8) == 'p' &&
                    cs.charAt(i + 9) == 'l' &&
                    cs.charAt(i + 10) == 'e' &&
                    cs.charAt(i + 11) == '.' &&
                    cs.charAt(i + 12) == 'f' &&
                    cs.charAt(i + 13) == 'l' &&
                    cs.charAt(i + 14) == 'o' &&
                    cs.charAt(i + 15) == 'a' &&
                    cs.charAt(i + 16) == 't' &&
                    cs.charAt(i + 17) == 'w' &&
                    cs.charAt(i + 18) == 'i' &&
                    cs.charAt(i + 19) == 'n' &&
                    cs.charAt(i + 20) == 'd' &&
                    cs.charAt(i + 21) == 'o' &&
                    cs.charAt(i + 22) == 'w' &&
                    cs.charAt(i + 23) == 'd' &&
                    cs.charAt(i + 24) == 'e' &&
                    cs.charAt(i + 25) == 'm' &&
                    cs.charAt(i + 26) == 'o') {
                return true;
            }
        }
        return false;
    }
}
