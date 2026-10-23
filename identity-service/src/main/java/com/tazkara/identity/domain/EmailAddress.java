package com.tazkara.identity.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Locale;
import java.util.Objects;


@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EmailAddress {

    @Column(name = "email", nullable = false, length = 254)
    private String value;

    public static EmailAddress of(String raw) {
        Objects.requireNonNull(raw, "email must not be null");
        return new EmailAddress(raw.trim().toLowerCase(Locale.ROOT));
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
