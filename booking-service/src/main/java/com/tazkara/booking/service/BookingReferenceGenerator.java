package com.tazkara.booking.service;

import com.tazkara.booking.domain.BookingReference;
import com.tazkara.booking.repository.BookingRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;


@Component
public class BookingReferenceGenerator {

    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int LENGTH = 8;
    private static final int MAX_ATTEMPTS = 5;

    private final SecureRandom random = new SecureRandom();
    private final BookingRepository bookingRepository;

    public BookingReferenceGenerator(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public BookingReference next() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = randomReference();
            if (!bookingRepository.existsByReferenceValue(candidate)) {
                return BookingReference.of(candidate);
            }
        }
        throw new IllegalStateException("Could not generate a unique booking reference");
    }

    String randomReference() {
        StringBuilder builder = new StringBuilder(BookingReference.PREFIX);
        for (int i = 0; i < LENGTH; i++) {
            builder.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return builder.toString();
    }
}
