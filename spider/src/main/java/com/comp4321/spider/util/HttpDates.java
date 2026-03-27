package com.comp4321.spider.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;

public final class HttpDates {
    private static final DateTimeFormatter RFC_1123 = DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.US);

    private HttpDates() {}

    public static String formatRfc1123(Instant instant) {
        return RFC_1123.format(ZonedDateTime.ofInstant(instant, ZoneOffset.UTC));
    }

    public static Optional<Instant> parseRfc1123(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(ZonedDateTime.parse(value, RFC_1123).toInstant());
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }
}

