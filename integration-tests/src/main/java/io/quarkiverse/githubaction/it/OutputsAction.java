package io.quarkiverse.githubaction.it;

import io.quarkiverse.githubaction.Action;
import io.quarkiverse.githubaction.Outputs;

public class OutputsAction {

    public static final String ACTION_NAME = "OutputsAction";
    public static final String OUTPUT_KEY = "testOutputKey";
    public static final String OUTPUT_VALUE = "test output value";

    @Action(ACTION_NAME)
    void test(Outputs outputs) {
        outputs.add(OUTPUT_KEY, OUTPUT_VALUE);
    }
}
