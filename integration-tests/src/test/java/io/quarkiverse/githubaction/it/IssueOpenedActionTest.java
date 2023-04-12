package io.quarkiverse.githubaction.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;

import io.quarkiverse.githubaction.Context;
import io.quarkiverse.githubaction.ContextInitializer;
import io.quarkiverse.githubaction.Inputs;
import io.quarkiverse.githubaction.InputsInitializer;
import io.quarkiverse.githubaction.it.IssueOpenedActionTest.IssueOpenedActionTestProfile;
import io.quarkiverse.githubaction.testing.DefaultTestContext;
import io.quarkiverse.githubaction.testing.DefaultTestInputs;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
@TestProfile(IssueOpenedActionTestProfile.class)
public class IssueOpenedActionTest {

    @Test
    @Launch(value = {})
    public void testLaunchCommand(LaunchResult result) {
        assertThat(result.getOutput()).contains(IssueOpenedAction.TEST_OUTPUT);
        assertThat(result.getOutput()).contains("Repository: yrodiere/quarkus-bot-java-playground");
        assertThat(result.getOutput()).contains("Issue title: Test issue title");
    }

    public static class IssueOpenedActionTestProfile implements QuarkusTestProfile {

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
            return new DefaultTestInputs(Map.of(Inputs.ACTION, IssueOpenedAction.ACTION_NAME));
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
                    return Thread.currentThread().getContextClassLoader().getResource("/payloads/issue-opened.json").getFile();
                }

                @Override
                public String getGitHubEventName() {
                    return "issues";
                }
            };
        }
    }
}
