package io.quarkiverse.githubaction.runtime;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.Outputs;

/**
 * See https://docs.github.com/en/actions/learn-github-actions/workflow-commands-for-github-actions#setting-an-output-parameter
 */
class OutputsImpl implements Outputs {

    private final Map<String, String> outputs = new LinkedHashMap<>();

    OutputsImpl() {
    }

    @Override
    public void produce(String key, String value) {
        if (outputs.containsKey(key)) {
            throw new IllegalArgumentException(key + " is already defined as an output with value " + outputs.get(key));
        }

        outputs.put(key, value);
    }

    public void produce(Commands commands) {
        for (Entry<String, String> outputEntry : outputs.entrySet()) {
            commands.setOutput(outputEntry.getKey(), outputEntry.getValue());
        }
    }
}
