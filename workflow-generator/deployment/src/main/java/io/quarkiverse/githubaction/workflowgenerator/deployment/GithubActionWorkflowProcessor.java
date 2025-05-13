package io.quarkiverse.githubaction.workflowgenerator.deployment;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkiverse.githubaction.workflowgenerator.spi.GeneratedGithubActionWorkflowResourceBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedFileSystemResourceBuildItem;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

public class GithubActionWorkflowProcessor {

    private static final Logger LOG = Logger.getLogger(GithubActionWorkflowProcessor.class);
    private static final String FEATURE = "github-action-workflow-generator";
    private static final String DEFAULT_JAVA_VERSION = "21";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void generateWorkflow(GithubActionWorkflowConfiguration config,
            BuildProducer<GeneratedGithubActionWorkflowResourceBuildItem> workflow) {
        if (!config.generation().enabled()) {
            LOG.info("Github Action Workflow generation is disabled. Skipping.");
            return;
        }
        Path projectRootDir = Projects.getProjectRoot();
        Engine engine = Engine.builder().addDefaults().build();
        Template template = engine.parse(getTemplateContent("workflow.yml.qute"));

        String fileName = "build.yml";
        Map<String, Object> params = Map.<String, Object> of(
                "name", "Build (Generated)",
                "jobName", "Build",
                "runner", config.runner(),
                "jdkDistribution", config.jdk().distribution(),
                "jdkVersion",
                config.jdk().version().orElse(getJavaVersion(projectRootDir).orElse(DEFAULT_JAVA_VERSION)),
                "hashFiles", hashFiles(projectRootDir),
                "buildCommand", buildCommand(projectRootDir),
                "testCommand", testCommand(projectRootDir));

        TemplateInstance templateInstance = template.data(params);
        String content = templateInstance.render();
        workflow.produce(new GeneratedGithubActionWorkflowResourceBuildItem(fileName, content));
    }

    @BuildStep
    void saveWorkflow(List<GeneratedGithubActionWorkflowResourceBuildItem> items,
            BuildProducer<GeneratedFileSystemResourceBuildItem> fileSystemResources) {
        Path projectRootDir = Projects.getProjectRoot();
        Path workflowDir = projectRootDir.resolve(".github").resolve("workflows");

        for (var item : items) {
            Path workflowFile = workflowDir.resolve(item.getName());
            String resourcePath = workflowFile.toAbsolutePath().toString();
            fileSystemResources.produce(new GeneratedFileSystemResourceBuildItem(resourcePath, item.getContent().getBytes()));
        }
    }

    private boolean hasMavenWrapper(Path projectDir) {
        return projectDir.resolve("mvnw").toFile().exists();
    }

    private boolean hasGradleWrapper(Path projectDir) {
        return projectDir.resolve("gradlew").toFile().exists();
    }

    private String hashFiles(Path projectDir) {
        BuildTool buildTool = QuarkusProjectHelper.detectExistingBuildTool(projectDir);
        return switch (buildTool) {
            case MAVEN -> "**/pom.xml";
            case GRADLE -> "**/build.gradle*";
            default -> throw new IllegalStateException("Unexpected value: " + buildTool);
        };
    }

    private String buildCommand(Path projectDir) {
        BuildTool buildTool = QuarkusProjectHelper.detectExistingBuildTool(projectDir);
        return switch (buildTool) {
            case MAVEN ->
                hasMavenWrapper(projectDir) ? "./mvnw clean package -DskipTests=true" : "mvn clean package -DskipTests=true";
            case GRADLE -> hasGradleWrapper(projectDir) ? "./gradlew build" : "gradle clean build";
            default -> throw new IllegalStateException("Unexpected value: " + buildTool);
        };
    }

    private String testCommand(Path projectDir) {
        BuildTool buildTool = QuarkusProjectHelper.detectExistingBuildTool(projectDir);
        return switch (buildTool) {
            case MAVEN -> hasMavenWrapper(projectDir) ? "./mvnw verify" : "mvn verify";
            case GRADLE -> hasGradleWrapper(projectDir) ? "./gradlew test" : "gradle test";
            default -> throw new IllegalStateException("Unexpected value: " + projectDir);
        };
    }

    private Optional<String> getJavaVersion(Path projectDir) {
        try {
            QuarkusProject project = QuarkusProjectHelper.getProject(projectDir);
            return Optional.of(String.valueOf(project.getJavaVersion().getAsInt()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Path getWorkingDirectory() {
        return Paths.get(System.getProperty("user.dir"));
    }

    private String getTemplateContent(String resourcePath) {
        try (var is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null)
                throw new IllegalArgumentException("Template not found: " + resourcePath);
            return new String(is.readAllBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read template: " + resourcePath, e);
        }
    }
}
