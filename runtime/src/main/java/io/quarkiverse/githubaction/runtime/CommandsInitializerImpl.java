package io.quarkiverse.githubaction.runtime;

import javax.inject.Singleton;

import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.CommandsInitializer;

@Singleton
public class CommandsInitializerImpl implements CommandsInitializer {

    @Override
    public Commands createCommands() {
        return new CommandsImpl(System.getenv());
    }
}
