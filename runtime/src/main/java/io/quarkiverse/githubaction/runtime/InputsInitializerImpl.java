package io.quarkiverse.githubaction.runtime;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkiverse.githubaction.Inputs;
import io.quarkiverse.githubaction.InputsInitializer;
import tools.jackson.databind.json.JsonMapper;

@Singleton
public class InputsInitializerImpl implements InputsInitializer {

    @Inject
    JsonMapper objectMapper;

    @Override
    public Inputs createInputs() {
        return new InputsImpl(objectMapper);
    }
}
