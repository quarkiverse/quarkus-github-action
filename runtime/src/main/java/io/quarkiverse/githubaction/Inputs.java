package io.quarkiverse.githubaction;

import java.util.Map;
import java.util.Optional;

/**
 * Inputs provided to the action.
 */
public interface Inputs {

    String ACTION = "action";
    String GITHUB_TOKEN = "github-token";

    Map<String, String> all();

    default String get(String key) {
        return all().get(key);
    }

    default String getRequired(String key) {
        String value = all().get(key);

        if (value == null) {
            throw new IllegalArgumentException("Input " + key + " is required and has not been provided");
        }

        return get(key);
    }

    default String getOrDefault(String key, String defaultValue) {
        return all().getOrDefault(key, defaultValue);
    }

    default String getAction() {
        return all().getOrDefault(ACTION, Action.UNNAMED);
    }

    default Optional<String> getGitHubToken() {
        return Optional.ofNullable(all().get(GITHUB_TOKEN));
    }
}
