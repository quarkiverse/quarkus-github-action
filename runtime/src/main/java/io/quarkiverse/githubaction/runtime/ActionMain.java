package io.quarkiverse.githubaction.runtime;

import java.io.File;

import javax.inject.Inject;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.CommandsInitializer;
import io.quarkiverse.githubaction.Context;
import io.quarkiverse.githubaction.ContextInitializer;
import io.quarkiverse.githubaction.Inputs;
import io.quarkiverse.githubaction.InputsInitializer;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.runtime.annotations.RegisterForReflection;

@QuarkusMain
public class ActionMain implements QuarkusApplication {

    private static final Logger LOG = Logger.getLogger(ActionMain.class);

    @Inject
    ContextInitializer contextInitializer;

    @Inject
    InputsInitializer inputsInitializer;

    @Inject
    CommandsInitializer commandsInitializer;

    @Inject
    PayloadTypeResolver payloadTypeResolver;

    @Inject
    GitHubEventHandler gitHubEventHandler;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public int run(String... args) throws Exception {
        try {
            Context context = contextInitializer.createContext();
            Inputs inputs = inputsInitializer.createInputs();
            OutputsImpl outputs = new OutputsImpl();
            Commands commands = commandsInitializer.createCommands();

            GitHubEvent gitHubEvent = new GitHubEvent(inputs.getAction(), context,
                    getEventAction(context),
                    inputs, outputs, commands,
                    payloadTypeResolver.getPayloadType(context.getGitHubEventName()));

            gitHubEventHandler.handle(gitHubEvent);

            outputs.produce(commands);

            return 0;
        } catch (Exception e) {
            LOG.error("An error occured while executing the action", e);
            return 1;
        }
    }

    private String getEventAction(Context context) {
        // unfortunately, the action is not included in the context so we have to read it from the payload
        GenericPayload genericPayload = null;
        if (context.getGitHubEventPath() != null) {
            try {
                genericPayload = objectMapper.readValue(new File(context.getGitHubEventPath()), GenericPayload.class);
            } catch (Exception e) {
                LOG.warnf("Error extracting the event action from the payload %s", context.getGitHubEventPath());
            }
        }
        return genericPayload != null ? genericPayload.getAction() : null;
    }

    @RegisterForReflection
    private static class GenericPayload {

        private String action;

        public String getAction() {
            return action;
        }
    }
}
