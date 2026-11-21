package com.example.catalog.service;

import com.example.catalog.dto.BomLine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BomCsvParserTest {

    private final BomCsvParser parser = new BomCsvParser();

    @Test
    void parsesFlexibleHeadersAndQuotedCommas() {
        String csv = "Designator,Part Number,Description,Qty\n"
                + "R1,,\"Resistor, 10k 0402 1%\",100\n"
                + "U1,LM358,,\"1,500\"\n";

        List<BomLine> rows = parser.parse(csv);

        assertEquals(2, rows.size());
        assertEquals("R1", rows.get(0).reference());
        assertNull(rows.get(0).mpn());
        assertEquals("Resistor, 10k 0402 1%", rows.get(0).description());
        assertEquals(100, rows.get(0).quantity());
        assertEquals("LM358", rows.get(1).mpn());
        assertEquals(1500, rows.get(1).quantity());
    }

    @Test
    void detectsSemicolonDelimiter_andSkipsBlankLines() {
        String csv = "Description;MPN;Manufacturer;Quantity\r\n"
                + "Timer IC;NE555;Texas Instruments;10\r\n"
                + "\r\n";

        List<BomLine> rows = parser.parse(csv);

        assertEquals(1, rows.size());
        assertEquals("NE555", rows.get(0).mpn());
        assertEquals("Texas Instruments", rows.get(0).manufacturer());
    }

    @Test
    void missingDescriptionAndMpnColumns_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("Reference,Qty\nR1,10\n"));
    }

    @Test
    void invalidQuantity_isRejectedWithLineNumber() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> parser.parse("Description,Qty\nResistor,many\n"));
        assertTrue(ex.getMessage().contains("Line 2"));
    }

    @Test
    void emptyFile_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("  "));
    }
}
