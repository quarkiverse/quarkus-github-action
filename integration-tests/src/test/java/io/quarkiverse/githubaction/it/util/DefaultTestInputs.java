package io.quarkiverse.githubaction.it.util;

import java.util.Optional;

import io.quarkiverse.githubaction.Inputs;

public class DefaultTestInputs implements Inputs {

    @Override
    public String get(String key) {
        return null;
    }

    @Override
    public String getAction() {
        return null;
    }

    @Override
    public Optional<String> getGitHubToken() {
        return Optional.empty();
    }
}
