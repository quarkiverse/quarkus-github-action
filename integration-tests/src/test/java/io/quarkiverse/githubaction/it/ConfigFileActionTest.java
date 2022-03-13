package io.quarkiverse.githubaction.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.Set;

import javax.enterprise.inject.Alternative;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GitHub;

import io.quarkiverse.githubaction.GitHubFileDownloader;
import io.quarkiverse.githubaction.Inputs;
import io.quarkiverse.githubaction.InputsInitializer;
import io.quarkiverse.githubaction.it.ConfigFileActionTest.ConfigFileActionTestProfile;
import io.quarkiverse.githubaction.it.util.DefaultTestInputs;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
@TestProfile(ConfigFileActionTestProfile.class)
public class ConfigFileActionTest {

    @Test
    @Launch(value = {})
    public void testLaunchCommand(LaunchResult result) {
        assertThat(result.getOutput()).contains("Value 1: test value 1");
        assertThat(result.getOutput()).contains("Value 2: test value 2");
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
            return new DefaultTestInputs() {
                @Override
                public String getAction() {
                    return ConfigFileAction.ACTION_NAME;
                }
            };
        }
    }

    @Alternative
    @Singleton
    public static class MockGitHubFileDownloader implements GitHubFileDownloader {

        @Override
        public Optional<String> getFileContent(GitHub gitHub, String repository, String fullPath) {
            if (!fullPath.equals(".github/example-config-file.yml")) {
                return Optional.empty();
            }
            return Optional.of("{ 'value1': 'test value 1', 'value2': 'test value 2'}");
        }
    }
}
