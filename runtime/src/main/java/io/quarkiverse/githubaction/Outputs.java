package io.quarkiverse.githubaction;

/**
 * Define outputs for the action.
 */
public interface Outputs {

    void produce(String key, String value);

    /**
     * Use {@link #produce(String, String)} instead.
     */
    @Deprecated(since = "0.3.0", forRemoval = true)
    default void add(String key, String value) {
        produce(key, value);
    }
}
