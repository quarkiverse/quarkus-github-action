package io.quarkiverse.githubaction.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.condition.EnabledIf;

import io.quarkiverse.githubaction.Context;
import io.quarkiverse.githubaction.ContextInitializer;
import io.quarkiverse.githubaction.Inputs;
import io.quarkiverse.githubaction.InputsInitializer;
import io.quarkiverse.githubaction.it.RepositoryActionTest.RepositoryActionTestProfile;
import io.quarkiverse.githubaction.testing.DefaultTestContext;
import io.quarkiverse.githubaction.testing.DefaultTestInputs;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
@TestProfile(RepositoryActionTestProfile.class)
public class RepositoryActionTest {
    private static final boolean LOCAL = System.getenv("CURRENT_REPO") == null;
    private static final String REPOSITORY_NAME = LOCAL ? "my/local-repo" : System.getenv("CURRENT_REPO");
    private static final String GITHUB_TOKEN = System.getenv("CURRENT_TOKEN");

    @Test
    @Launch(value = {})
    @DisabledIf("isLocal")
    public void shouldPrintRepositoryNameWhenRunningOnGitHub(LaunchResult result) {
        assertThat(result.getOutput()).contains(REPOSITORY_NAME);
    }

    @Test
    @Launch(value = {}, exitCode = 1)
    @EnabledIf("isLocal")
    public void shouldFailWhenRunningLocally(LaunchResult result) {
        assertThat(result.getErrorOutput()).contains(REPOSITORY_NAME);
    }

    public static class RepositoryActionTestProfile implements QuarkusTestProfile {
        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            var alternatives = new HashSet<Class<?>>();
            alternatives.add(MockInputsInitializer.class);
            if (LOCAL) {
                alternatives.add(MockContextInitializer.class);
            }
            return alternatives;
        }
    }

    @Alternative
    @Singleton
    public static class MockInputsInitializer implements InputsInitializer {
        @Override
        public Inputs createInputs() {
            var inputs = new HashMap<String, String>();
            inputs.put(Inputs.ACTION, RepositoryAction.ACTION_NAME);
            if (!LOCAL) {
                inputs.put(Inputs.GITHUB_TOKEN, GITHUB_TOKEN);
            }
            return new DefaultTestInputs(inputs);
        }
    }

    @Alternative
    @Singleton
    public static class MockContextInitializer implements ContextInitializer {
        @Override
        public Context createContext() {
            return new DefaultTestContext() {
                @Override
                public String getGitHubRepository() {
                    return REPOSITORY_NAME;
                }
            };
        }
    }

    private static boolean isLocal() {
        return LOCAL;
    }
}
