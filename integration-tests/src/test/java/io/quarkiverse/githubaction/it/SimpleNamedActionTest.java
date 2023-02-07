package io.quarkiverse.githubaction.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;

import io.quarkiverse.githubaction.Inputs;
import io.quarkiverse.githubaction.InputsInitializer;
import io.quarkiverse.githubaction.it.SimpleNamedActionTest.SimpleNamedActionTestProfile;
import io.quarkiverse.githubaction.testing.DefaultTestInputs;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
@TestProfile(SimpleNamedActionTestProfile.class)
public class SimpleNamedActionTest {

    @Test
    @Launch(value = {})
    public void testLaunchCommand(LaunchResult result) {
        assertThat(result.getOutput()).contains(SimpleNamedAction.TEST_OUTPUT);
    }

    public static class SimpleNamedActionTestProfile implements QuarkusTestProfile {

        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(MockInputsInitializer.class);
        }
    }

    @Alternative
    @Singleton
    public static class MockInputsInitializer implements InputsInitializer {

        @Override
        public Inputs createInputs() {
            return new DefaultTestInputs(Map.of(Inputs.ACTION, SimpleNamedAction.ACTION_NAME));
        }
    }
}
