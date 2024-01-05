package io.quarkiverse.githubaction;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Inputs provided to the action.
 */
public interface Inputs {

    String ACTION = "action";
    String GITHUB_TOKEN = "github-token";

    Map<String, String> all();

    default Optional<String> get(String key) {
        String value = all().get(key);

        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(value);
    }

    default String getRequired(String key) {
        return get(key).orElseThrow(() -> new IllegalStateException("Input " + key + " is required and has not been provided"));
    }

    default Optional<Boolean> getBoolean(String key) {
        String value = all().get(key);

        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(Boolean.parseBoolean(value));
    }

    default boolean getRequiredBoolean(String key) {
        return getBoolean(key)
                .orElseThrow(() -> new IllegalStateException("Input " + key + " is required and has not been provided"));
    }

    default OptionalLong getLong(String key) {
        String value = all().get(key);

        if (value == null || value.isBlank()) {
            return OptionalLong.empty();
        }

        return OptionalLong.of(Long.valueOf(value));
    }

    default long getRequiredLong(String key) {
        return getLong(key)
                .orElseThrow(() -> new IllegalStateException("Input " + key + " is required and has not been provided"));
    }

    @Deprecated(forRemoval = true)
    default OptionalInt getInteger(String key) {
        return getInt(key);
    }

    default OptionalInt getInt(String key) {
        String value = all().get(key);

        if (value == null || value.isBlank()) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(Integer.valueOf(value));
    }

    default int getRequiredInt(String key) {
        return getInt(key)
                .orElseThrow(() -> new IllegalStateException("Input " + key + " is required and has not been provided"));
    }

    default String getAction() {
        String action = all().get(ACTION);

        if (action == null || action.isBlank()) {
            return Action.UNNAMED;
        }

        return action;
    }

    default Optional<String> getGitHubToken() {
        return get(GITHUB_TOKEN);
    }
}
