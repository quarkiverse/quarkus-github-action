package io.quarkiverse.githubaction.it;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog;
import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language;
import io.quarkus.devtools.commands.CreateProject.CreateProjectKey;
import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;

public class CodestartKotlinTest {
    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
            .languages(QuarkusCodestartCatalog.Language.KOTLIN)
            .setupStandaloneExtensionTest("io.quarkiverse.githubaction:quarkus-github-action")
            .putData(CreateProjectKey.PROJECT_NAME, "My action name")
            .putData(CreateProjectKey.PROJECT_DESCRIPTION, "My action description")
            .build();

    @Test
    void testContent() throws Throwable {
        codestartTest.checkGeneratedSource("org.acme.MyAction");
        codestartTest.assertThatGeneratedFileMatchSnapshot(Language.KOTLIN, "pom.xml");
        codestartTest.assertThatGeneratedFileMatchSnapshot(Language.KOTLIN, "README.md");
        codestartTest.assertThatGeneratedFileMatchSnapshot(Language.KOTLIN, "action.yml");
        codestartTest.assertThatGeneratedFileMatchSnapshot(Language.KOTLIN, "action.docker.yml");
        codestartTest.assertThatGeneratedTreeMatchSnapshots(Language.KOTLIN, ".github/");
        codestartTest.assertThatGeneratedFileMatchSnapshot(Language.KOTLIN, ".github/dependabot.yml");
        codestartTest.assertThatGeneratedFileMatchSnapshot(Language.KOTLIN, ".github/workflows/ci.yml");
        codestartTest.assertThatGeneratedFileMatchSnapshot(Language.KOTLIN, ".github/workflows/publish-action-artifact.yml");
        codestartTest.assertThatGeneratedFileMatchSnapshot(Language.KOTLIN, ".github/workflows/release.yml");
        codestartTest.assertThatGeneratedFileMatchSnapshot(Language.KOTLIN, "src/main/resources/application.properties");
    }

    @Test
    void buildAllProjects() throws Throwable {
        codestartTest.buildAllProjects();
    }
}