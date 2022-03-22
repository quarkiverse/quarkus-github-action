package io.quarkiverse.githubaction.runtime;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import io.quarkiverse.githubaction.Outputs;

/**
 * See https://docs.github.com/en/actions/learn-github-actions/workflow-commands-for-github-actions#setting-an-output-parameter
 */
public class OutputsImpl implements Outputs {

    private final Map<String, String> outputs = new LinkedHashMap<>();

    public OutputsImpl() {
    }

    @Override
    public void produce(String key, String value) {
        if (outputs.containsKey(key)) {
            throw new IllegalArgumentException(key + " is already defined as an output with value " + outputs.get(key));
        }

        outputs.put(key, value);
    }

    public void produce() {
        if (outputs.isEmpty()) {
            return;
        }

        // make sure the set-output commands will be on a new line
        System.out.println();
        for (Entry<String, String> outputEntry : outputs.entrySet()) {
            System.out.println("::set-output name=" + outputEntry.getKey() + "::" + outputEntry.getValue());
        }
    }
}
