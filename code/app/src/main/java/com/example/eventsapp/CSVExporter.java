package com.example.eventsapp;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 * Utility class to handle exporting data to a CSV file.
 */
public class CSVExporter {

    public interface CsvRowMapper<T> {
        String[] mapToRow(T item);
    }

    public static <T> boolean exportToCsv(Context context, Uri uri, String[] headers, List<T> items, CsvRowMapper<T> mapper) {
        try (OutputStream os = context.getContentResolver().openOutputStream(uri);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os))) {

            // Write Headers
            writer.write(TextUtils.join(",", headers));
            writer.newLine();

            // Write Data Rows
            for (T item : items) {
                String[] row = mapper.mapToRow(item);
                for (int i = 0; i < row.length; i++) {
                    row[i] = escapeCsv(row[i]);
                }
                writer.write(TextUtils.join(",", row));
                writer.newLine();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Escapes a string to be written properly to a CSV.
     * Wraps in quotes if there are commas, newlines, or quotes.
     */
    private static String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}