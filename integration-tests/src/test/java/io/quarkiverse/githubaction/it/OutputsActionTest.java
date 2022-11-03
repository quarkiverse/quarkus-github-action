package io.quarkiverse.githubaction.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.Alternative;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;

import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.CommandsInitializer;
import io.quarkiverse.githubaction.Inputs;
import io.quarkiverse.githubaction.InputsInitializer;
import io.quarkiverse.githubaction.it.OutputsActionTest.OutputsActionTestProfile;
import io.quarkiverse.githubaction.it.util.DefaultTestInputs;
import io.quarkiverse.githubaction.runtime.CommandsImpl;
import io.quarkiverse.githubaction.runtime.github.EnvFiles;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
@TestProfile(OutputsActionTestProfile.class)
public class OutputsActionTest {

    @Test
    @Launch(value = {})
    public void testLaunchCommand(LaunchResult result) throws IOException {
        assertThat(Path.of(System.getProperty("java.io.tmpdir") + "/temp-github-output.txt")).content()
                .contains("testOutputKey=test output value" + System.lineSeparator() +
                        "testOutputKey2=test output value 2" + System.lineSeparator());
    }

    public static class OutputsActionTestProfile implements QuarkusTestProfile {

        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(MockInputsInitializer.class, MockCommandsInitializer.class);
        }
    }

    @Alternative
    @Singleton
    public static class MockInputsInitializer implements InputsInitializer {

        @Override
        public Inputs createInputs() {
            return new DefaultTestInputs(Map.of(Inputs.ACTION, OutputsAction.ACTION_NAME));
        }
    }

    @Alternative
    @Singleton
    public static class MockCommandsInitializer implements CommandsInitializer {

        @Override
        public Commands createCommands() {
            try {
                Path githubOutputPath = Path.of(System.getProperty("java.io.tmpdir") + "/temp-github-output.txt");
                Files.deleteIfExists(githubOutputPath);

                System.out.println(githubOutputPath);

                return new CommandsImpl(Map.of(EnvFiles.GITHUB_OUTPUT, githubOutputPath.toString()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
