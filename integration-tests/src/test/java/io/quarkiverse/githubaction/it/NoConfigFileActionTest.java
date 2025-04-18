package io.quarkiverse.githubaction.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GitHub;

import io.quarkiverse.githubaction.GitHubFileDownloader;
import io.quarkiverse.githubaction.Inputs;
import io.quarkiverse.githubaction.InputsInitializer;
import io.quarkiverse.githubaction.it.NoConfigFileActionTest.ConfigFileActionTestProfile;
import io.quarkiverse.githubaction.testing.DefaultTestInputs;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
@TestProfile(ConfigFileActionTestProfile.class)
public class NoConfigFileActionTest {

    @Test
    @Launch(value = {})
    public void testLaunchCommand(LaunchResult result) {
        assertThat(result.getOutput()).contains("Config file bean: null");
    }

    public static class ConfigFileActionTestProfile implements QuarkusTestProfile {

        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(MockInputsInitializer.class, MockGitHubFileDownloader.class);
        }
    }

    @Alternative
    @Singleton
    public static class MockInputsInitializer implements InputsInitializer {

        @Override
        public Inputs createInputs() {
            return new DefaultTestInputs(Map.of(Inputs.ACTION, NoConfigFileAction.ACTION_NAME));
        }
    }

    @Alternative
    @Singleton
    public static class MockGitHubFileDownloader implements GitHubFileDownloader {

        @Override
        public Optional<String> getFileContent(GitHub gitHub, String repository, String fullPath) {
            return Optional.empty();
        }
    }
}
