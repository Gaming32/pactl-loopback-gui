package io.github.gaming32.pactlloopbackgui;

import io.github.gaming32.pactlloopbackgui.pactl.Pactl;
import io.github.gaming32.pactlloopbackgui.pactl.PactlModule;
import io.github.gaming32.pactlloopbackgui.pactl.PactlSourceOrSink;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws IOException {
        System.setProperty("pactl.commandWrapper", "bash -c");

        Pactl.listModules()
            .stream()
            .filter(module -> module.name().equals("module-loopback"))
            .map(PactlModule::index)
            .forEach(index -> {
                try {
                    Pactl.unloadModule(index);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });

        final var sources = new ArrayList<>(Pactl.listSources());
        final var sinks = Pactl.listSinks();

        final var sinkIds= sinks.stream().map(PactlSourceOrSink::index).collect(Collectors.toSet());
        sources.removeIf(s -> sinkIds.contains(s.index()));

        final var module = Pactl.loadModule("module-loopback", Map.of(
            "latency_msec", "20",
            "source", Integer.toString(sources.getFirst().index()),
            "sink", Integer.toString(sinks.getFirst().index())
        ));
        System.out.println("Loaded module " + module);
    }
}
