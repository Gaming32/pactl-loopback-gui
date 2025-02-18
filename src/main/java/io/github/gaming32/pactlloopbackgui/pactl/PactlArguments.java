package io.github.gaming32.pactlloopbackgui.pactl;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class PactlArguments {
    private PactlArguments() {
    }

    @Nullable
    public static Integer getInt(Map<String, String> arguments, String key) {
        return parseInt(arguments.get(key));
    }

    public static int getIntOrDefault(Map<String, String> arguments, String key, int defaultValue) {
        final var result = getInt(arguments, key);
        return result != null ? result : defaultValue;
    }

    @Nullable
    public static Integer putInt(Map<String, String> arguments, String key, int value) {
        return parseInt(arguments.put(key, Integer.toString(value)));
    }

    @Nullable
    private static Integer parseInt(@Nullable String value) {
        try {
            return value != null ? Integer.parseInt(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    public static Boolean getBoolean(Map<String, String> arguments, String key) {
        return parseBoolean(arguments.get(key));
    }

    public static boolean getBooleanOrDefault(Map<String, String> arguments, String key, boolean defaultValue) {
        final var result = getBoolean(arguments, key);
        return result != null ? result : defaultValue;
    }

    @Nullable
    public static Boolean putBoolean(Map<String, String> arguments, String key, boolean value) {
        return parseBoolean(arguments.put(key, value ? "y" : "n"));
    }

    @Nullable
    public static Boolean parseBoolean(@Nullable String value) {
        if (value == null) {
            return null;
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "1", "t", "y", "true", "yes", "on" -> true;
            case "0", "f", "n", "false", "no", "off" -> false;
            default -> null;
        };
    }

    public static String toString(Map<String, String> arguments) {
        return arguments.entrySet()
            .stream()
            .map(entry -> entry.getKey() + "=" + valueToString(entry.getValue()))
            .collect(Collectors.joining(" "));
    }

    private static String valueToString(String value) {
        if (value.codePoints().noneMatch(Character::isWhitespace)) {
            return value;
        }
        if (value.indexOf('"') != -1) {
            return '\'' + value + '\'';
        }
        return '"' + value + '"';
    }

    // Based on pa_modargs_new
    public static Map<String, String> parse(String arguments) throws IllegalArgumentException {
        final var result = new LinkedHashMap<String, String>();

        enum State {
            WHITESPACE, KEY, VALUE_START, VALUE_SIMPLE, VALUE_DOUBLE_QUOTES, VALUE_TICKS
        }

        var keyStart = 0;
        var keyLength = 0;
        var valueStart = 0;
        var valueLength = 0;

        var state = State.WHITESPACE;
        for (var i = 0; i < arguments.length(); ) {
            final var c = arguments.codePointAt(i);
            switch (state) {
                case WHITESPACE -> {
                    if (c == '=') {
                        throw new IllegalArgumentException("Unexpected '=' at position " + i);
                    }
                    if (!Character.isWhitespace(c)) {
                        keyStart = i;
                        keyLength = 1;
                        state = State.KEY;
                    }
                }
                case KEY -> {
                    if (c == '=') {
                        state = State.VALUE_START;
                    } else {
                        keyLength++;
                    }
                }
                case VALUE_START -> {
                    if (c == '\'') {
                        state = State.VALUE_TICKS;
                        valueStart = i + 1;
                        valueLength = 0;
                    } else if (c == '"') {
                        state = State.VALUE_DOUBLE_QUOTES;
                        valueStart = i + 1;
                        valueLength = 0;
                    } else if (Character.isWhitespace(c)) {
                        if (result.put(arguments.substring(keyStart, keyStart + keyLength), "") != null) {
                            throw new IllegalArgumentException("Duplicate key at position " + keyStart);
                        }
                        state = State.WHITESPACE;
                    } else {
                        state = State.VALUE_SIMPLE;
                        valueStart = i;
                        valueLength = 1;
                    }
                }
                case VALUE_SIMPLE -> {
                    if (Character.isWhitespace(c)) {
                        if (result.put(
                            arguments.substring(keyStart, keyStart + keyLength),
                            arguments.substring(valueStart, valueStart + valueLength)
                        ) != null) {
                            throw new IllegalArgumentException("Duplicate key at position " + keyStart);
                        }
                        state = State.WHITESPACE;
                    } else {
                        valueLength++;
                    }
                }
                case VALUE_DOUBLE_QUOTES -> {
                    if (c == '"') {
                        if (result.put(
                            arguments.substring(keyStart, keyStart + keyLength),
                            arguments.substring(valueStart, valueStart + valueLength)
                        ) != null) {
                            throw new IllegalArgumentException("Duplicate key at position " + keyStart);
                        }
                        state = State.WHITESPACE;
                    } else {
                        valueLength++;
                    }
                }
                case VALUE_TICKS -> {
                    if (c == '\'') {
                        if (result.put(
                            arguments.substring(keyStart, keyStart + keyLength),
                            arguments.substring(valueStart, valueStart + valueLength)
                        ) != null) {
                            throw new IllegalArgumentException("Duplicate key at position " + keyStart);
                        }
                        state = State.WHITESPACE;
                    } else {
                        valueLength++;
                    }
                }
            }
            i += Character.charCount(c);
        }

        if (state == State.VALUE_START) {
            if (result.put(arguments.substring(keyStart, keyStart + keyLength), "") != null) {
                throw new IllegalArgumentException("Duplicate key at position " + keyStart);
            }
        } else if (state == State.VALUE_SIMPLE) {
            if (result.put(
                arguments.substring(keyStart, keyStart + keyLength),
                arguments.substring(valueStart)
            ) != null) {
                throw new IllegalArgumentException("Duplicate key at position " + keyStart);
            }
        } else if (state != State.WHITESPACE) {
            throw new IllegalArgumentException("Unexpected end of argument");
        }

        return result;
    }
}
