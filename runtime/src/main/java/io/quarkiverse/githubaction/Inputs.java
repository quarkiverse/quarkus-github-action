package io.quarkiverse.githubaction;

import java.util.Optional;

/**
 * Inputs provided to the action.
 */
public interface Inputs {

    String get(String key);

    String getAction();

    Optional<String> getGitHubToken();
}
