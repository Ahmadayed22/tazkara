package com.tazkara.booking.service.csv;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;


public class CsvWriter {

    private static final String LINE_SEPARATOR = "\r\n";

    private final Writer writer;

    public CsvWriter(Writer writer) {
        this.writer = writer;
    }

    public void writeRow(List<String> fields) throws IOException {
        writer.write(fields.stream().map(CsvWriter::escape).collect(Collectors.joining(",")));
        writer.write(LINE_SEPARATOR);
    }

    public static String escape(String field) {
        if (field == null) {
            return "";
        }
        String value = field;
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) {
            value = "'" + value;
        }
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        return needsQuotes ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }
}
