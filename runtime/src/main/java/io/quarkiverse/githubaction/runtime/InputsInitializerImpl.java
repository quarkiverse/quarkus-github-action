package io.quarkiverse.githubaction.runtime;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.githubaction.Inputs;
import io.quarkiverse.githubaction.InputsInitializer;

@Singleton
public class InputsInitializerImpl implements InputsInitializer {

    @Inject
    ObjectMapper objectMapper;

    @Override
    public Inputs createInputs() {
        return new InputsImpl(objectMapper);
    }
}
