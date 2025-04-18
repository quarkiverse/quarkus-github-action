package io.quarkiverse.githubaction.it;

import io.quarkiverse.githubaction.Action;
import io.quarkiverse.githubaction.ConfigFile;

public class NoConfigFileAction {

    public static final String ACTION_NAME = "NoConfigFileAction";
    public static final String TEST_OUTPUT = NoConfigFileAction.class.getSimpleName() + " - Test output";

    @Action(ACTION_NAME)
    void test(@ConfigFile("non-existent-config-file.yml") ConfigFileBean configFileBean) {
        System.out.println("Config file bean: " + configFileBean);
    }

    public static class ConfigFileBean {

        public String value1;

        public String value2;
    }
}
