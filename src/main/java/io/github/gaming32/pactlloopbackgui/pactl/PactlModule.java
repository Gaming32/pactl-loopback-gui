package io.github.gaming32.pactlloopbackgui.pactl;

import java.util.Map;

public record PactlModule(int index, String name, Map<String, String> arguments) {
    @Override
    public String toString() {
        return index + "\t" + name + "\t" + PactlArguments.toString(arguments);
    }
}
