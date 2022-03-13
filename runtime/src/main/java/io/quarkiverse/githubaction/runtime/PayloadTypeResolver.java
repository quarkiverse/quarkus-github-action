package io.quarkiverse.githubaction.runtime;

import org.kohsuke.github.GHEventPayload;

public interface PayloadTypeResolver {

    Class<? extends GHEventPayload> getPayloadType(String event);
}
