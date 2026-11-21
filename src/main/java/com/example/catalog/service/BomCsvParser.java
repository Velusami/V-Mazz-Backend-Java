package com.example.catalog.service;

import com.example.catalog.dto.BomLine;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Reads a BOM from CSV. Column names are flexible (Qty / Quantity, Part Number / MPN, ...),
 * the delimiter may be comma, semicolon or tab, and quoted fields may contain commas.
 */
@Component
public class BomCsvParser {

    private static final Set<String> MPN = Set.of(
            "mpn", "partnumber", "partno", "partnum", "manufacturerpartnumber", "mfrpartnumber", "mfrpn");
    private static final Set<String> DESCRIPTION = Set.of(
            "description", "desc", "item", "itemname", "value", "partdescription", "name", "component");
    private static final Set<String> MANUFACTURER = Set.of(
            "manufacturer", "mfr", "mfg", "make", "brand", "manufacturername");
    private static final Set<String> QUANTITY = Set.of(
            "quantity", "qty", "qnty", "count");
    private static final Set<String> REFERENCE = Set.of(
            "reference", "references", "designator", "designators", "refdes", "ref");

    public List<BomLine> parse(InputStream in) throws IOException {
        return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
    }

    List<BomLine> parse(String text) {

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("The CSV file is empty");
        }
        if (text.charAt(0) == '\uFEFF') {          // Excel adds a byte-order mark
            text = text.substring(1);
        }

        List<List<String>> records = splitRecords(text, detectDelimiter(text));
        if (records.isEmpty()) {
            throw new IllegalArgumentException("The CSV file is empty");
        }

        List<String> header = records.get(0).stream().map(BomCsvParser::normalise).toList();
        int iMpn = indexOf(header, MPN);
        int iDesc = indexOf(header, DESCRIPTION);
        int iMfr = indexOf(header, MANUFACTURER);
        int iQty = indexOf(header, QUANTITY);
        int iRef = indexOf(header, REFERENCE);

        if (iMpn < 0 && iDesc < 0) {
            throw new IllegalArgumentException(
                    "The CSV header must contain a 'description' or 'mpn' column. Found: " + records.get(0));
        }

        List<BomLine> rows = new ArrayList<>();
        for (int i = 1; i < records.size(); i++) {
            List<String> r = records.get(i);
            rows.add(new BomLine(
                    get(r, iRef),
                    get(r, iDesc),
                    get(r, iMpn),
                    get(r, iMfr),
                    parseQuantity(get(r, iQty), i + 1)));
        }

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("The CSV file has a header but no rows");
        }
        return rows;
    }

    // ------------------------------------------------------------------

    private static String normalise(String header) {
        return header == null ? "" : header.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static int indexOf(List<String> header, Set<String> aliases) {
        for (int i = 0; i < header.size(); i++) {
            if (aliases.contains(header.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static String get(List<String> record, int index) {
        if (index < 0 || index >= record.size()) {
            return null;
        }
        String value = record.get(index).trim();
        return value.isEmpty() ? null : value;
    }

    private static Integer parseQuantity(String value, int lineNumber) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(",", "").replace(" ", "")).intValueExact();
        } catch (ArithmeticException | NumberFormatException ex) {
            throw new IllegalArgumentException("Line " + lineNumber + ": invalid quantity '" + value + "'");
        }
    }

    private static char detectDelimiter(String text) {
        int end = text.indexOf('\n');
        String first = end < 0 ? text : text.substring(0, end);
        int commas = count(first, ',');
        int semicolons = count(first, ';');
        int tabs = count(first, '\t');
        if (semicolons > commas && semicolons >= tabs) {
            return ';';
        }
        if (tabs > commas && tabs > semicolons) {
            return '\t';
        }
        return ',';
    }

    private static int count(String text, char c) {
        int n = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == c) {
                n++;
            }
        }
        return n;
    }

    /** Splits CSV text into records and fields; handles quotes, doubled quotes and newlines inside quotes. */
    static List<List<String>> splitRecords(String text, char delimiter) {

        List<List<String>> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == delimiter) {
                fields.add(field.toString());
                field.setLength(0);
            } else if (c == '\n') {
                fields.add(field.toString());
                field.setLength(0);
                addRecord(records, fields);
                fields = new ArrayList<>();
            } else if (c != '\r') {
                field.append(c);
            }
        }

        fields.add(field.toString());
        addRecord(records, fields);
        return records;
    }

    // Skip completely blank lines.
    private static void addRecord(List<List<String>> records, List<String> fields) {
        boolean blank = fields.stream().allMatch(f -> f.trim().isEmpty());
        if (!blank) {
            records.add(fields);
        }
    }
}
