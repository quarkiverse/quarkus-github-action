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
        return Optional.ofNullable(all().get(key));
    }

    default String getRequired(String key) {
        String value = all().get(key);

        if (value == null) {
            throw new IllegalStateException("Input " + key + " is required and has not been provided");
        }

        return value;
    }

    default Optional<Boolean> getBoolean(String key) {
        String value = all().get(key);

        if (value == null) {
            return Optional.empty();
        }

        return Optional.of(Boolean.parseBoolean(value));
    }

    default OptionalLong getLong(String key) {
        String value = all().get(key);

        if (value == null) {
            return OptionalLong.empty();
        }

        return OptionalLong.of(Long.valueOf(value));
    }

    default OptionalInt getInteger(String key) {
        String value = all().get(key);

        if (value == null) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(Integer.valueOf(value));
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
