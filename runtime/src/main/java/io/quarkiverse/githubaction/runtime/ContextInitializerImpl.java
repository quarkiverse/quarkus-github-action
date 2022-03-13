package io.quarkiverse.githubaction.runtime;

import javax.inject.Singleton;

import io.quarkiverse.githubaction.Context;
import io.quarkiverse.githubaction.ContextInitializer;

@Singleton
public class ContextInitializerImpl implements ContextInitializer {

    @Override
    public Context createContext() {
        return new ContextImpl();
    }
}
