package io.quarkiverse.githubaction.it;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog;
import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language;
import io.quarkus.devtools.commands.CreateProject.CreateProjectKey;
import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;

public class CodestartTest {
    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
            .languages(QuarkusCodestartCatalog.Language.JAVA)
            .setupStandaloneExtensionTest("io.quarkiverse.githubaction:quarkus-github-action")
            .putData(CreateProjectKey.PROJECT_NAME, "My action name")
            .putData(CreateProjectKey.PROJECT_DESCRIPTION, "My action description")
            .build();

    @Test
    void testContent() throws Throwable {
        codestartTest.checkGeneratedSource("org.acme.MyAction");
        codestartTest.assertThatGeneratedFileMatchSnapshot(Language.JAVA, "pom.xml");
        codestartTest.assertThatGeneratedFileMatchSnapshot(Language.JAVA, "README.md");
        codestartTest.assertThatGeneratedFileMatchSnapshot(Language.JAVA, "action.yml");
        codestartTest.assertThatGeneratedFileMatchSnapshot(Language.JAVA, "action.docker.yml");
        codestartTest.assertThatGeneratedTreeMatchSnapshots(Language.JAVA, ".github/");
        codestartTest.assertThatGeneratedFileMatchSnapshot(Language.JAVA, ".github/dependabot.yml");
        codestartTest.assertThatGeneratedFileMatchSnapshot(Language.JAVA, ".github/workflows/ci.yml");
        codestartTest.assertThatGeneratedFileMatchSnapshot(Language.JAVA, ".github/workflows/publish-action-artifact.yml");
        codestartTest.assertThatGeneratedFileMatchSnapshot(Language.JAVA, "src/main/resources/application.properties");
    }

    @Test
    void buildAllProjects() throws Throwable {
        codestartTest.buildAllProjects();
    }
}