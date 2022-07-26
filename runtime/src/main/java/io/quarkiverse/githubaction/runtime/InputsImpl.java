package io.quarkiverse.githubaction.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.githubaction.Inputs;

class InputsImpl implements Inputs {

    private static final String INPUT_PREFIX = "INPUT_";
    private static final String JSON_INPUTS = "JSON_INPUTS";

    private final Map<String, String> inputs;

    InputsImpl(ObjectMapper objectMapper) {
        String jsonInputs = System.getenv(JSON_INPUTS);
        final Map<String, String> tmpInputs;

        if (jsonInputs != null && !jsonInputs.isBlank()) {
            // when using a composite action, inputs are not propagated
            // we propagate them as JSON
            try {
                tmpInputs = objectMapper.readValue(jsonInputs, new TypeReference<Map<String, String>>() {
                });
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Unable to parse JSON inputs to a map", e);
            }
        } else {
            // when using a Docker action, we can extract the inputs from the environment variables
            tmpInputs = new HashMap<>();

            for (Entry<String, String> envEntry : System.getenv().entrySet()) {
                if (!envEntry.getKey().startsWith(INPUT_PREFIX)) {
                    continue;
                }

                tmpInputs.put(envEntry.getKey().substring(INPUT_PREFIX.length()).toLowerCase(Locale.ROOT), envEntry.getValue());
            }
        }

        this.inputs = Collections.unmodifiableMap(tmpInputs);
    }

    @Override
    public Map<String, String> all() {
        return inputs;
    }
}
