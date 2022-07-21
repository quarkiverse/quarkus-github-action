package io.quarkiverse.githubaction;

/**
 * Define outputs for the action.
 */
public interface Outputs {

    void produce(String key, String value);
}
