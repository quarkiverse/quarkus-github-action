package io.quarkiverse.githubaction.it;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
public class SimpleActionTest {

    @Test
    @Launch(value = {})
    public void testLaunchCommand(LaunchResult result) {
        assertThat(result.getOutput()).contains(SimpleAction.TEST_OUTPUT);
    }
}
