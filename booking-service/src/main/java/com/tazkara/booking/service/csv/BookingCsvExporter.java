package com.tazkara.booking.service.csv;

import com.tazkara.booking.domain.Booking;
import com.tazkara.booking.domain.BookingLine;
import org.springframework.stereotype.Component;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * US-16: writes confirmed bookings as CSV, one row per ticket type of each booking, using Java I/O
 * (ByteArrayOutputStream -> OutputStreamWriter (UTF-8) -> BufferedWriter).
 */
@Component
public class BookingCsvExporter {

    static final List<String> HEADER = List.of(
            "reference", "attendee_email", "ticket_type", "quantity", "unit_price", "line_total", "confirmed_at");

    public byte[] export(List<Booking> confirmedBookings) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(bytes, StandardCharsets.UTF_8))) {
            CsvWriter csv = new CsvWriter(writer);
            csv.writeRow(HEADER);
            for (Booking booking : confirmedBookings) {
                for (BookingLine line : booking.getLines()) {
                    csv.writeRow(List.of(
                            booking.getReference().value(),
                            booking.getAttendeeEmail(),
                            line.ticketTypeName(),
                            String.valueOf(line.quantity()),
                            line.unitPrice().toPlainString(),
                            line.lineTotal().toPlainString(),
                            DateTimeFormatter.ISO_INSTANT.format(booking.getConfirmedAt())));
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not write CSV export", ex);
        }
        return bytes.toByteArray();
    }
}
