package io.quarkiverse.githubaction.it;

import io.quarkiverse.githubaction.Action;

public class SimpleNamedAction {

    public static final String ACTION_NAME = "SimpleNamedAction";
    public static final String TEST_OUTPUT = SimpleNamedAction.class.getSimpleName() + " - Test output";

    @Action(ACTION_NAME)
    void test() {
        System.out.println(TEST_OUTPUT);
    }
}
