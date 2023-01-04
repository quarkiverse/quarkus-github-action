package io.quarkiverse.githubaction.testing;

import java.util.Collections;
import java.util.Map;

import io.quarkiverse.githubaction.Inputs;

public class DefaultTestInputs implements Inputs {

    private final Map<String, String> inputs;

    public DefaultTestInputs(Map<String, String> inputs) {
        this.inputs = Collections.unmodifiableMap(inputs);
    }

    @Override
    public Map<String, String> all() {
        return inputs;
    }
}
