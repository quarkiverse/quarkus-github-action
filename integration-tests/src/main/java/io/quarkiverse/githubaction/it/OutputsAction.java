package io.quarkiverse.githubaction.it;

import io.quarkiverse.githubaction.Action;
import io.quarkiverse.githubaction.Outputs;

public class OutputsAction {

    public static final String ACTION_NAME = "OutputsAction";
    public static final String OUTPUT_KEY = "testOutputKey";
    public static final String OUTPUT_VALUE = "test output value";
    public static final String OUTPUT_KEY_2 = "testOutputKey2";
    public static final String OUTPUT_VALUE_2 = "test output value 2";

    @Action(ACTION_NAME)
    void test(Outputs outputs) {
        outputs.produce(OUTPUT_KEY, OUTPUT_VALUE);
        outputs.produce(OUTPUT_KEY_2, OUTPUT_VALUE_2);
    }
}
