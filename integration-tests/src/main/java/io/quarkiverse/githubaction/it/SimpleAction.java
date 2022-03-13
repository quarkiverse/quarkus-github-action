package io.quarkiverse.githubaction.it;

import io.quarkiverse.githubaction.Action;

public class SimpleAction {

    public static final String TEST_OUTPUT = SimpleAction.class.getSimpleName() + " - Test output";

    @Action
    void test() {
        System.out.println(TEST_OUTPUT);
    }
}
