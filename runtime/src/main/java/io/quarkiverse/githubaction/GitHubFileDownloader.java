package io.quarkiverse.githubaction;

import java.util.Optional;

import org.kohsuke.github.GitHub;

public interface GitHubFileDownloader {

    Optional<String> getFileContent(GitHub gitHub, String repository, String fullPath);
}