package io.quarkiverse.githubaction.runtime;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.runtime.github.EnvFiles;

/**
 * See https://docs.github.com/en/actions/learn-github-actions/workflow-commands-for-github-actions
 */
public class CommandsImpl implements Commands {

    private static final Logger LOG = Logger.getLogger(CommandsImpl.class);

    private Map<String, String> env;
    private String currentStopCommandsMarker;

    public CommandsImpl(Map<String, String> env) {
        this.env = env;
    }

    @Override
    public void setOutput(String name, String value) {
        appendEnvFile(EnvFiles.GITHUB_OUTPUT, name + "=" + value);
    }

    @Override
    public void debug(String message) {
        command("::debug::" + message);
    }

    @Override
    public void notice(String message) {
        notice(message, null, null, null, null, null, null);
    }

    @Override
    public void notice(String message, String title, String file, Integer line, Integer endLine, Integer col,
            Integer endColumn) {
        message("notice", message, title, file, line, endLine, col, endColumn);
    }

    @Override
    public void warning(String message) {
        warning(message, null, null, null, null, null, null);
    }

    @Override
    public void warning(String message, String title, String file, Integer line, Integer endLine, Integer col,
            Integer endColumn) {
        message("warning", message, title, file, line, endLine, col, endColumn);
    }

    @Override
    public void error(String message) {
        error(message, null, null, null, null, null, null);
    }

    @Override
    public void error(String message, String title, String file, Integer line, Integer endLine, Integer col,
            Integer endColumn) {
        message("error", message, title, file, line, endLine, col, endColumn);
    }

    @Override
    public void group(String title) {
        command("::group::" + title);
    }

    @Override
    public void echo(String message) {
        command(message);
    }

    @Override
    public void endGroup() {
        command("::endgroup::");
    }

    @Override
    public void addMask(String value) {
        command("::add-mask::" + value);
    }

    @Override
    public void stopCommands() {
        this.currentStopCommandsMarker = "stopCommandsMarker-" + UUID.randomUUID();
        command("::stop-commands::" + this.currentStopCommandsMarker);
    }

    @Override
    public void pursueCommands() {
        if (this.currentStopCommandsMarker == null) {
            throw new IllegalStateException("Cannot pursue commands if no stop commands marker is defined");
        }

        command("::" + this.currentStopCommandsMarker + "::");
        this.currentStopCommandsMarker = null;
    }

    @Override
    public void echoOn() {
        command("::echo::on");
    }

    @Override
    public void echoOff() {
        command("::echo::off");
    }

    @Override
    public void saveState(String name, String value) {
        appendEnvFile(EnvFiles.GITHUB_STATE, name + "=" + value);
    }

    @Override
    public void environmentVariable(String name, String value) {
        if (!value.contains("\n")) {
            appendEnvFile(EnvFiles.GITHUB_ENV, name + "=" + value);
        } else {
            appendEnvFile(EnvFiles.GITHUB_ENV,
                    name + "<<EOF" + System.lineSeparator() + value + System.lineSeparator() + "EOF");
        }
    }

    @Override
    public void jobSummary(String markdownContent) {
        writeEnvFile(EnvFiles.GITHUB_STEP_SUMMARY, markdownContent);
    }

    @Override
    public void appendJobSummary(String markdownContent) {
        appendEnvFile(EnvFiles.GITHUB_STEP_SUMMARY, markdownContent);
    }

    @Override
    public void removeJobSummary() {
        try {
            Files.deleteIfExists(getEnvFilePath(EnvFiles.GITHUB_STEP_SUMMARY));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to delete job summary", e);
        }
    }

    @Override
    public void systemPath(String path) {
        appendEnvFile(EnvFiles.GITHUB_PATH, path);
    }

    private void message(String level, String message, String title, String file, Integer line, Integer endLine, Integer col,
            Integer endColumn) {
        StringBuilder command = new StringBuilder();
        command.append("::" + level);

        Map<String, String> parameters = new LinkedHashMap<>();
        if (file != null && !file.isBlank()) {
            parameters.put("file", file);
        }
        if (line != null) {
            parameters.put("line", String.valueOf(line));
        }
        if (endLine != null) {
            parameters.put("endLine", String.valueOf(endLine));
        }
        if (col != null) {
            parameters.put("col", String.valueOf(col));
        }
        if (endColumn != null) {
            parameters.put("endColumn", String.valueOf(endColumn));
        }
        if (title != null && !title.isBlank()) {
            parameters.put("title", title);
        }

        command.append(parameters.keySet().stream()
                .map(key -> key + "=" + parameters.get(key))
                .collect(Collectors.joining(",", " ", "")));
        command.append("::");
        command.append(message);

        command(command.toString());
    }

    private void command(String command) {
        System.out.println(command);
    }

    private void appendEnvFile(String fileName, String content) {
        writeEnvFile(fileName, content, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private void writeEnvFile(String fileName, String content, OpenOption... openOptions) {
        Path path = getEnvFilePath(fileName);

        try {
            Files.writeString(path, content + System.lineSeparator(), openOptions);

            LOG.debugf("Wrote %s in environment file %s", content, path);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write content to file " + fileName + " at path " + path, e);
        }
    }

    private Path getEnvFilePath(String fileName) {
        String envFileName = env.get(fileName);

        if (envFileName == null || envFileName.isBlank()) {
            throw new IllegalStateException("No path defined for environment file " + fileName);
        }

        Path path = Paths.get(envFileName);
        return path;
    }
}
