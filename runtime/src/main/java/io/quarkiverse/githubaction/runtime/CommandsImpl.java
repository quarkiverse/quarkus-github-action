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

import io.quarkiverse.githubaction.Commands;

/**
 * See https://docs.github.com/en/actions/learn-github-actions/workflow-commands-for-github-actions
 */
public class CommandsImpl implements Commands {

    private static final String GITHUB_PATH = "GITHUB_PATH";
    private static final String GITHUB_ENV = "GITHUB_ENV";
    private static final String GITHUB_STEP_SUMMARY = "GITHUB_STEP_SUMMARY";

    private String currentStopCommandsMarker;

    CommandsImpl() {
    }

    @Override
    public void setOutput(String name, String value) {
        command("::set-output name=" + name + "::" + value);
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
        command("::save-state name=" + name + "::" + value);
    }

    @Override
    public void environmentVariable(String name, String value) {
        if (!value.contains("\n")) {
            writeEnvFile(GITHUB_ENV, name + "=" + value, StandardOpenOption.APPEND);
        } else {
            writeEnvFile(GITHUB_ENV, name + "<<EOF" + System.lineSeparator() + value + System.lineSeparator() + "EOF",
                    StandardOpenOption.APPEND);
        }
    }

    @Override
    public void jobSummary(String markdownContent) {
        writeEnvFile(GITHUB_STEP_SUMMARY, markdownContent);
    }

    @Override
    public void appendJobSummary(String markdownContent) {
        writeEnvFile(GITHUB_STEP_SUMMARY, markdownContent, StandardOpenOption.APPEND);
    }

    @Override
    public void removeJobSummary() {
        try {
            Files.deleteIfExists(getEnvFilePath(GITHUB_STEP_SUMMARY));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to delete job summary", e);
        }
    }

    @Override
    public void systemPath(String path) {
        writeEnvFile(GITHUB_PATH, path, StandardOpenOption.APPEND);
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

    private void writeEnvFile(String fileName, String content, OpenOption... openOptions) {
        Path path = getEnvFilePath(fileName);

        try {
            Files.writeString(path, System.lineSeparator() + content, openOptions);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write content to file " + fileName + " at path " + path, e);
        }
    }

    private Path getEnvFilePath(String fileName) {
        String envFileName = System.getenv(fileName);

        if (envFileName == null || envFileName.isBlank()) {
            throw new IllegalStateException("No path defined for environment file " + fileName);
        }

        Path path = Paths.get(envFileName);
        return path;
    }
}
