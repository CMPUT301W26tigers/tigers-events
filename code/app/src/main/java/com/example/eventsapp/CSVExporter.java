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

    /**
     * Strategy interface that converts a domain object into an ordered array of CSV field
     * values. Implementations define which fields are exported and in what order, matching
     * the column order declared in the header row.
     *
     * @param <T> the type of object to convert
     */
    public interface CsvRowMapper<T> {
        /**
         * Maps a single domain object to its CSV column values.
         *
         * @param item the object to convert
         * @return an array of raw field values in column order; {@code null} entries are
         *         written as empty strings
         */
        String[] mapToRow(T item);
    }

    /**
     * Writes {@code items} to the file pointed to by {@code uri} in CSV format, using the
     * provided {@code headers} as the first row and {@code mapper} to convert each item.
     *
     * <p>Each field value is properly escaped per RFC 4180: values containing commas,
     * double-quotes, or newlines are wrapped in double-quotes, and any embedded
     * double-quotes are doubled.
     *
     * @param <T>     the type of items to export
     * @param context Android context used to open the output stream via the content resolver
     * @param uri     the destination URI (e.g. from {@code ACTION_CREATE_DOCUMENT})
     * @param headers column header labels; must match the array length returned by the mapper
     * @param items   the list of objects to export
     * @param mapper  converts each item to a row of raw string values
     * @return {@code true} if the file was written successfully; {@code false} on any I/O or
     *         content-resolver error
     */
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