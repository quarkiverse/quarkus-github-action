package io.quarkiverse.githubaction.workflowgenerator.deployment;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Projects {

    private static final String[] BUILD_FILES = { "pom.xml", "build.gradle", "build.gradle.kts" };
    private static final String[] SCM_ROOT_FILES = { ".git", ".hg", ".svn" };

    public static Path getProjectRoot() {
        return getProjectRoot(Paths.get(System.getProperty("user.dir")));
    }

    /**
     * Get the root {@link Path} of the project.
     * Iterates over the parent directories and returns the last directory that contains a build file.
     * If no build file is found, the current directory is returned.
     *
     * @param dir the directory to start the search from
     * @return the root directory of the project
     */
    public static Path getProjectRoot(Path dir) {
        Path currentDir = dir;
        Path lastProjectDir = null;
        while (currentDir != null) {
            boolean buildFileFound = hasBuildFile(currentDir);
            if (!buildFileFound && lastProjectDir != null) {
                return lastProjectDir;
            }
            if (isScmRoot(currentDir)) {
                return currentDir;
            }
            currentDir = currentDir.getParent();
        }
        return dir;
    }

    private static boolean isScmRoot(Path dir) {
        for (String scmRootFile : SCM_ROOT_FILES) {
            if (dir.resolve(scmRootFile).toFile().exists()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasBuildFile(Path dir) {
        for (String buildFile : BUILD_FILES) {
            if (dir.resolve(buildFile).toFile().exists()) {
                return true;
            }
        }
        return false;
    }
}
