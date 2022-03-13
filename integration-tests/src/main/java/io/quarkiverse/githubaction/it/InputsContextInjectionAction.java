package io.quarkiverse.githubaction.it;

import io.quarkiverse.githubaction.Action;
import io.quarkiverse.githubaction.Context;
import io.quarkiverse.githubaction.Inputs;

public class InputsContextInjectionAction {

    public static final String ACTION_NAME = "InputsContextInjectionAction";

    @Action(ACTION_NAME)
    void test(Inputs inputs, Context context) {
        System.out.println("Inputs - Action: " + inputs.getAction());
        System.out.println("Context - Event: " + context.getGitHubEventName());
    }
}
