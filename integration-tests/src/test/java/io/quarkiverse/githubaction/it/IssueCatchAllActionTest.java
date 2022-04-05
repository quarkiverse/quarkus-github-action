package io.quarkiverse.githubaction.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import javax.enterprise.inject.Alternative;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;

import io.quarkiverse.githubaction.Context;
import io.quarkiverse.githubaction.ContextInitializer;
import io.quarkiverse.githubaction.Inputs;
import io.quarkiverse.githubaction.InputsInitializer;
import io.quarkiverse.githubaction.it.IssueCatchAllActionTest.IssueCatchAllActionTestProfile;
import io.quarkiverse.githubaction.it.util.DefaultTestContext;
import io.quarkiverse.githubaction.it.util.DefaultTestInputs;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
@TestProfile(IssueCatchAllActionTestProfile.class)
public class IssueCatchAllActionTest {

    @Test
    @Launch(value = {})
    public void testLaunchCommand(LaunchResult result) {
        assertThat(result.getOutput()).contains(IssueCatchAllAction.TEST_OUTPUT);
        assertThat(result.getOutput()).contains("Repository: yrodiere/quarkus-bot-java-playground");
        assertThat(result.getOutput()).contains("Issue title: Test issue title");
    }

    public static class IssueCatchAllActionTestProfile implements QuarkusTestProfile {

        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(MockInputsInitializer.class, MockContextInitializer.class);
        }
    }

    @Alternative
    @Singleton
    public static class MockInputsInitializer implements InputsInitializer {

        @Override
        public Inputs createInputs() {
            return new DefaultTestInputs() {
                @Override
                public String getAction() {
                    return IssueCatchAllAction.ACTION_NAME;
                }
            };
        }
    }

    @Alternative
    @Singleton
    public static class MockContextInitializer implements ContextInitializer {

        @Override
        public Context createContext() {
            return new DefaultTestContext() {

                @Override
                public String getGitHubEventPath() {
                    return System.getProperty("test.payloads.path") + "/issue-opened.json";
                }

                @Override
                public String getGitHubEventName() {
                    return "issues";
                }
            };
        }
    }
}
