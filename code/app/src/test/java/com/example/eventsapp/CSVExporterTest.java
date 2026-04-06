package com.example.eventsapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for CSV Export logic.
 * Hits edge cases for CSV formatting and data mapping.
 */
public class CSVExporterTest {

    private CSVExporter.CsvRowMapper<Entrant> entrantMapper;

    @Before
    public void setUp() {
        // Mocking the mapper logic used in OrganizerEnrolledFragment
        entrantMapper = entrant -> new String[]{
                entrant.getName(),
                entrant.getEmail(),
                entrant.getStatus() != null ? entrant.getStatus().name() : "ACCEPTED"
        };
    }

    @Test
    public void testEscapeCsv_SimpleValue() {
        assertEquals("John Doe", CSVExporter.escapeCsv("John Doe"));
    }

    @Test
    public void testEscapeCsv_NullValue() {
        // Edge Case: Null should return an empty string to keep columns aligned
        assertEquals("", CSVExporter.escapeCsv(null));
    }

    @Test
    public void testEscapeCsv_CommaInValue() {
        // Edge Case: Value with comma must be wrapped in quotes
        String input = "Smith, John";
        String expected = "\"Smith, John\"";
        assertEquals(expected, CSVExporter.escapeCsv(input));
    }

    @Test
    public void testEscapeCsv_QuotesInValue() {
        // Edge Case: Quotes inside a name (e.g. nicknames)
        // RFC 4180: Double the internal quotes and wrap the whole thing in quotes.
        String input = "John \"The Legend\" Smith";
        String expected = "\"John \"\"The Legend\"\" Smith\"";
        assertEquals(expected, CSVExporter.escapeCsv(input));
    }

    @Test
    public void testEscapeCsv_NewlineInValue() {
        // Edge Case: Newlines shouldn't break the row format
        String input = "Line1\nLine2";
        String expected = "\"Line1\nLine2\"";
        assertEquals(expected, CSVExporter.escapeCsv(input));
    }

    @Test
    public void testMapper_CorrectColumnOrder() {
        // Pass Status.ACCEPTED explicitly here
        Entrant entrant = new Entrant("id123", "event123", "Jane Doe", "jane@test.com", Entrant.Status.ACCEPTED);
        String[] row = entrantMapper.mapToRow(entrant);

        assertEquals("Jane Doe", row[0]);
        assertEquals("jane@test.com", row[1]);
        assertEquals("ACCEPTED", row[2]);
    }

    @Test
    public void testMapper_HandlesEmptyEntrant() {
        // We provide nulls to the constructor
        Entrant emptyEntrant = new Entrant(null, null, null, null, null);

        // Note: The Entrant constructor defaults status to APPLIED if null is passed.
        // (The ternary operator: entrant.getStatus() != null ? ... : "ACCEPTED")
        emptyEntrant.setStatus(null);

        String[] row = entrantMapper.mapToRow(emptyEntrant);

        assertNotNull(row);
        assertEquals(3, row.length);

        // Now it should hit the "ACCEPTED" fallback in the mapper
        assertEquals("ACCEPTED", row[2]);

        // Also verify the null name/email from constructor were converted to empty strings by Entrant class
        assertEquals("", row[0]); // Entrant class converts null name to ""
        assertEquals("", row[1]); // Entrant class converts null email to ""
    }

    @Test
    public void testUtf8Characters() {
        // Edge Case: Non-Latin characters (Internationalization)
        String input = "José Ørd";
        assertEquals("José Ørd", CSVExporter.escapeCsv(input));
    }
}