package com.example.subsystemdiscovery.visualization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SummaryFormatterServiceTest {

    private final SummaryFormatterService formatterService = new SummaryFormatterService();

    @Test
    public void testCleanAndFormatMarkdown() {
        String input = "<think>\nSome thinking process here.\n</think>\n" +
                       "<thought>\nSome other thought block.\n</thought>\n" +
                       "B</thought>\n" +
                       "# Subsystem Boundary Analysis\n" +
                       "This is a **bold** paragraph.\n" +
                       "\n" +
                       "- Item 1\n" +
                       "- Item 2\n" +
                       "\n" +
                       "| Header 1 | Header 2 |\n" +
                       "| --- | --- |\n" +
                       "| Cell 1 | Cell 2 |";

        String result = formatterService.format(input);

        // Verify HTML output contains expected tags
        // Heading
        assertContains(result, "<h1>Subsystem Boundary Analysis</h1>");
        // Paragraph with bold text
        assertContains(result, "<p>This is a <strong>bold</strong> paragraph.</p>");
        // List items
        assertContains(result, "<li>Item 1</li>");
        assertContains(result, "<li>Item 2</li>");
        // Tables
        assertContains(result, "<table>");
        assertContains(result, "<th>Header 1</th>");
        assertContains(result, "<td>Cell 1</td>");

        // Verify it cleaned the thinking/thought tags and hallucinated B</thought>
        assertNotContains(result, "think");
        assertNotContains(result, "thought");
    }

    @Test
    public void testNullAndEmptySummary() {
        assertEquals("", formatterService.format(null));
        assertEquals("", formatterService.format(""));
        assertEquals("", formatterService.format("   "));
    }

    private void assertContains(String text, String substring) {
        if (!text.contains(substring)) {
            throw new AssertionError("Expected text to contain \"" + substring + "\", but was:\n" + text);
        }
    }

    private void assertNotContains(String text, String substring) {
        if (text.contains(substring)) {
            throw new AssertionError("Expected text NOT to contain \"" + substring + "\", but was:\n" + text);
        }
    }
}
