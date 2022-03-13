package io.quarkiverse.githubaction.it;

import io.quarkiverse.githubaction.Action;
import io.quarkiverse.githubaction.ConfigFile;

public class ConfigFileAction {

    public static final String ACTION_NAME = "SimpleActionWithName";
    public static final String TEST_OUTPUT = ConfigFileAction.class.getSimpleName() + " - Test output";

    @Action(ACTION_NAME)
    void test(@ConfigFile("example-config-file.yml") ConfigFileBean configFileBean) {
        System.out.println("Value 1: " + configFileBean.value1);
        System.out.println("Value 2: " + configFileBean.value2);
    }

    public static class ConfigFileBean {

        public String value1;

        public String value2;
    }
}
