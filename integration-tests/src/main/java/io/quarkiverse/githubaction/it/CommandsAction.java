package io.quarkiverse.githubaction.it;

import io.quarkiverse.githubaction.Action;
import io.quarkiverse.githubaction.Commands;

public class CommandsAction {

    public static final String ACTION_NAME = "CommandsAction";
    public static final String OUTPUT_KEY = "testOutputKey";
    public static final String OUTPUT_VALUE = "test output value";
    public static final String MULTILINE_OUTPUT_KEY = "testMultilineOutputKey";
    public static final String MULTILINE_OUTPUT_VALUE = "test\noutput\nvalue";

    @Action(ACTION_NAME)
    void test(Commands commands) {
        commands.setOutput(OUTPUT_KEY, OUTPUT_VALUE);
        commands.setOutput(MULTILINE_OUTPUT_KEY, MULTILINE_OUTPUT_VALUE);
    }
}
