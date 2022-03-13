package io.quarkiverse.githubaction.runtime;

import javax.inject.Singleton;

import io.quarkiverse.githubaction.Inputs;
import io.quarkiverse.githubaction.InputsInitializer;

@Singleton
public class InputsInitializerImpl implements InputsInitializer {

    @Override
    public Inputs createInputs() {
        return new InputsImpl();
    }
}
