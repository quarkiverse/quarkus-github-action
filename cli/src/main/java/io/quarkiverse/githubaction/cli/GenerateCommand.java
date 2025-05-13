package io.quarkiverse.githubaction.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.quarkiverse.githubaction.workflowgenerator.spi.GeneratedGithubActionWorkflowResourceBuildItem;
import io.quarkus.bootstrap.BootstrapAppModelFactory;
import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.deployment.builditem.GeneratedFileSystemResourceBuildItem;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.Dependency;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "generate", sortOptions = false, mixinStandardHelpOptions = false, header = "Generate Github Action Workflow for the current Quarkus project.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", optionListHeading = "%nOptions:%n")
public class GenerateCommand implements Callable<Integer> {

    private static final ArtifactDependency QUARKUS_GITHUB_ACTION = new ArtifactDependency("io.quarkiverse.githubaction",
            "quarkus-github-action-workflow-generator", null,
            "jar", GenerateCommand.getVersion());

    @Parameters(arity = "0..1", paramLabel = "GENERATION_PATH", description = " The path to generate the github action workflow files. Default is '.github'.")
    Optional<String> generationPath = Optional.of(".github");

    @Option(names = { "-h", "--help" }, usageHelp = true, description = "Display this help message.")
    public boolean help;

    public String[] getRequiredBuildItems() {
        return new String[] {
                GeneratedGithubActionWorkflowResourceBuildItem.class.getName(),
                GeneratedFileSystemResourceBuildItem.class.getName(),
        };
    };

    public Properties getBuildSystemProperties(Path outputDir) {
        Properties buildSystemProperties = new Properties();
        Path projectRoot = getWorkingDirectory();
        Path applicationPropertiesPath = projectRoot.resolve("src").resolve("main").resolve("resources")
                .resolve("application.properties");
        if (Files.exists(applicationPropertiesPath)) {
            try {
                buildSystemProperties.load(Files.newBufferedReader(applicationPropertiesPath));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        buildSystemProperties.put("quarkus.github-action.workflow.generation.enabled", "true");
        return buildSystemProperties;
    }

    public List<Dependency> getProjectDependencies() {
        List<Dependency> dependencies = new ArrayList<>();
        dependencies.add(QUARKUS_GITHUB_ACTION);
        try {
            BootstrapAppModelFactory.newInstance()
                    .setProjectRoot(getWorkingDirectory())
                    .setLocalProjectsDiscovery(true)
                    .resolveAppModel()
                    .getApplicationModel()
                    .getDependencies().forEach(d -> {
                        dependencies.add(new ArtifactDependency(d.getGroupId(), d.getArtifactId(), d.getClassifier(),
                                d.getType(), d.getVersion()));
                    });
        } catch (BootstrapException e) {
            // Ignore
        }
        return dependencies;
    }

    public Integer call() {
        Path projectRoot = getWorkingDirectory();
        Path outputDir = generationPath.map(Paths::get).orElse(projectRoot.resolve(".github")).resolve("workflows");

        if (outputDir.toFile().exists() && !outputDir.toFile().isDirectory()) {
            System.err.println("Output directory is not a directory: " + outputDir);
            return ExitCode.SOFTWARE;
        }
        if (!outputDir.toFile().exists() && !outputDir.toFile().mkdirs()) {
            System.err.println("Failed to create output directory: " + outputDir);
            return ExitCode.SOFTWARE;
        }

        BuildTool buildTool = QuarkusProjectHelper.detectExistingBuildTool(projectRoot);
        Path targetDirecotry = projectRoot.resolve(buildTool.getBuildDirectory());
        QuarkusBootstrap quarkusBootstrap = QuarkusBootstrap.builder()
                .setMode(QuarkusBootstrap.Mode.PROD)
                .setBuildSystemProperties(getBuildSystemProperties(outputDir))
                .setApplicationRoot(getWorkingDirectory())
                .setProjectRoot(getWorkingDirectory())
                .setTargetDirectory(targetDirecotry)
                .setIsolateDeployment(false)
                .setRebuild(true)
                .setTest(false)
                .setLocalProjectDiscovery(true)
                .setBaseClassLoader(ClassLoader.getSystemClassLoader())
                .setForcedDependencies(getProjectDependencies())
                .build();

        // Checking
        try (CuratedApplication curatedApplication = quarkusBootstrap.bootstrap()) {
            AugmentAction action = curatedApplication.createAugmentor();

            action.performCustomBuild(GithubActionWorkflowHandler.class.getName(),
                    new Consumer<List<GeneratedGithubActionWorkflowResourceBuildItem>>() {
                        @Override
                        public void accept(List<GeneratedGithubActionWorkflowResourceBuildItem> list) {
                            if (list.isEmpty()) {
                                System.out.println("No workflow files generated.");
                                return;
                            }
                            for (GeneratedGithubActionWorkflowResourceBuildItem item : list) {
                                Path workflowFile = outputDir.resolve(item.getName());
                                String resourcePath = workflowFile.toAbsolutePath().toString();
                                writeStringSafe(workflowFile, item.getContent());
                                System.out.printf("Generated workflow file: %s%n", resourcePath);
                            }
                        }
                    }, getRequiredBuildItems());

        } catch (BootstrapException e) {
            throw new RuntimeException(e);
        }
        return ExitCode.OK;
    }

    protected void writeStringSafe(Path p, String content) {
        try {
            Files.writeString(p, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Path getWorkingDirectory() {
        return Paths.get(System.getProperty("user.dir"));
    }

    private static String getVersion() {
        return read(GenerateCommand.class.getClassLoader().getResourceAsStream("version"));
    }

    private static String read(InputStream is) {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(is))) {
            return buffer.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
