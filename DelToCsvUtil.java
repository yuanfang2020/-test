import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 将任意分隔符文本（.del/.txt 等）转换成 CSV。
 */
public final class DelToCsvUtil {

    private DelToCsvUtil() {
    }

    /**
     * 将输入文件按指定分隔符转换成 CSV 文件。
     *
     * @param inputPath      输入 del 文件路径
     * @param outputPath     输出 csv 文件路径
     * @param delimiter      输入分隔符（如 '|', '\t', ';'）
     * @param charset        文件编码（null 时默认 UTF-8）
     * @param trimCell       是否去除单元格首尾空白
     * @param skipBlankLines 是否跳过空行
     */
    public static void delToCsv(
            Path inputPath,
            Path outputPath,
            char delimiter,
            Charset charset,
            boolean trimCell,
            boolean skipBlankLines
    ) throws IOException {
        if (inputPath == null || outputPath == null) {
            throw new IllegalArgumentException("inputPath/outputPath 不能为空");
        }
        if (!Files.exists(inputPath) || !Files.isRegularFile(inputPath)) {
            throw new IllegalArgumentException("输入文件不存在或不是普通文件: " + inputPath);
        }

        Charset actualCharset = charset == null ? StandardCharsets.UTF_8 : charset;
        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedReader reader = Files.newBufferedReader(inputPath, actualCharset);
             BufferedWriter writer = Files.newBufferedWriter(outputPath, actualCharset)) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (skipBlankLines && line.isBlank()) {
                    continue;
                }

                List<String> cells = splitLine(line, delimiter);
                if (trimCell) {
                    for (int i = 0; i < cells.size(); i++) {
                        cells.set(i, cells.get(i).trim());
                    }
                }

                writer.write(toCsvLine(cells));
                writer.newLine();
            }
        }
    }

    /**
     * 便捷重载：UTF-8、保留空行、不 trim。
     */
    public static void delToCsv(Path inputPath, Path outputPath, char delimiter) throws IOException {
        delToCsv(inputPath, outputPath, delimiter, StandardCharsets.UTF_8, false, false);
    }

    private static List<String> splitLine(String line, char delimiter) {
        List<String> result = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cell.append('"'); // 处理转义双引号
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == delimiter && !inQuotes) {
                result.add(cell.toString());
                cell.setLength(0);
            } else {
                cell.append(c);
            }
        }

        result.add(cell.toString());
        return result;
    }

    private static String toCsvLine(List<String> cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escapeCsv(cells.get(i)));
        }
        return sb.toString();
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        boolean needQuote = value.indexOf(',') >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;

        if (!needQuote) {
            return value;
        }

        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
