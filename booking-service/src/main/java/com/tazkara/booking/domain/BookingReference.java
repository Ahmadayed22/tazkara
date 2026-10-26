package com.tazkara.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;


@Embeddable
public class BookingReference {

    public static final String PREFIX = "TZK-";

    @Column(name = "reference", nullable = false, unique = true, length = 20)
    private String value;

    protected BookingReference() {
    }

    private BookingReference(String value) {
        this.value = value;
    }

    public static BookingReference of(String value) {
        Objects.requireNonNull(value, "reference must not be null");
        return new BookingReference(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BookingReference other && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
