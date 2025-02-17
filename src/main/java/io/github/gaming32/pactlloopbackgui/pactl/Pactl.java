package io.github.gaming32.pactlloopbackgui.pactl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Pactl {
    private static final Gson GSON = new GsonBuilder().create();
    private static final TypeToken<List<PactlSourceOrSink>> SOURCES_OR_SINKS_TYPE = new TypeToken<>() {};

    private static final String[] COMMAND_WRAPPER = Optional.ofNullable(System.getProperty("pactl.commandWrapper"))
        .map(wrapper -> wrapper.split(" "))
        .orElse(null);

    private Pactl() {
    }

    public static List<PactlSourceOrSink> listSources() throws IOException {
        return listSourcesOrSinks("sources");
    }

    public static List<PactlSourceOrSink> listSinks() throws IOException {
        return listSourcesOrSinks("sinks");
    }

    private static List<PactlSourceOrSink> listSourcesOrSinks(String what) throws IOException {
        try (var reader = runProcess("--format", "json", "list", what).inputReader()) {
            return GSON.fromJson(reader, SOURCES_OR_SINKS_TYPE.getType());
        }
    }

    public static List<PactlModule> listModules() throws IOException {
        try (var reader = runProcess("list", "short", "modules").inputReader()) {
            return reader.lines()
                .map(line -> line.split("\t", 4))
                .map(parts -> new PactlModule(Integer.parseInt(parts[0]), parts[1], PactlArguments.parse(parts[2])))
                .toList();
        }
    }

    public static int loadModule(String name) throws IOException {
        return loadModule(name, Map.of());
    }

    public static int loadModule(String name, Map<String, String> arguments) throws IOException {
        final var process = runProcess("load-module", name, PactlArguments.toString(arguments));
        try {
            if (process.waitFor() != 0) {
                try (var reader = process.errorReader()) {
                    throw new IOException(reader.readLine());
                }
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        try (var reader = process.inputReader()) {
            return Integer.parseInt(reader.readLine().trim());
        }
    }

    public static void unloadModule(int index) throws IOException {
        try {
            runProcess("unload-module", Integer.toString(index)).waitFor();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Process runProcess(String... command) throws IOException {
        final String[] fullCommand;
        if (COMMAND_WRAPPER != null) {
            fullCommand = Arrays.copyOf(COMMAND_WRAPPER, COMMAND_WRAPPER.length + 1);
            fullCommand[COMMAND_WRAPPER.length] = "pactl " + String.join(" ", command);
        } else {
            fullCommand = command;
        }

        return new ProcessBuilder(fullCommand).start();
    }
}
