package io.github.gaming32.pactlloopbackgui.pactl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
        try (var reader = runProcess("--format", "json", "list", what)) {
            return GSON.fromJson(reader, SOURCES_OR_SINKS_TYPE);
        }
    }

    public static List<PactlModule> listModules() throws IOException {
        try (var reader = runProcess("list", "short", "modules")) {
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
        try (var reader = runProcess("load-module", name, PactlArguments.toString(arguments))) {
            return Integer.parseInt(reader.readLine().trim());
        }
    }

    public static void unloadModule(int index) throws IOException {
        runProcess("unload-module", Integer.toString(index)).close();
    }

    private static BufferedReader runProcess(String... command) throws IOException {
        final String[] fullCommand;
        if (COMMAND_WRAPPER != null) {
            fullCommand = Arrays.copyOf(COMMAND_WRAPPER, COMMAND_WRAPPER.length + 1);
            fullCommand[COMMAND_WRAPPER.length] = "pactl " + String.join(" ", command);
        } else {
            fullCommand = new String[command.length + 1];
            fullCommand[0] = "pactl";
            System.arraycopy(command, 0, fullCommand, 1, command.length);
        }

        final var process = new ProcessBuilder(fullCommand).start();
        return new BufferedReader(process.inputReader()) {
            @Override
            public void close() throws IOException {
                super.close();
                try {
                    if (process.waitFor() != 0) {
                        try (var reader = process.errorReader()) {
                            throw new IOException(reader.lines().collect(Collectors.joining("\n")));
                        }
                    }
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }
}
