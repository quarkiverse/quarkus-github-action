package io.quarkiverse.githubaction.runtime.github;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import io.quarkiverse.githubaction.GitHubFileDownloader;
import io.quarkus.runtime.LaunchMode;

@Singleton
public class GitHubFileDownloaderImpl implements GitHubFileDownloader {

    private static final Logger LOG = Logger.getLogger(GitHubFileDownloaderImpl.class);

    @Inject
    LaunchMode launchMode;

    @SuppressWarnings("deprecation")
    @Override
    public Optional<String> getFileContent(GitHub gitHub, String repository, String fullPath) {
        if (gitHub.isOffline()) {
            throw new IllegalStateException(
                    "A connected GitHub API client is necessary to read a config file and no token was provided. Please provide a token as input of the action.");
        }

        try {
            GHRepository ghRepository = gitHub.getRepository(repository);
            GHContent ghContent = ghRepository.getFileContent(fullPath);

            return Optional.of(ghContent.getContent());
        } catch (GHFileNotFoundException e) {
            // The config being not found can be perfectly acceptable, we log a warning in dev and test modes.
            // Note that you will have a GHFileNotFoundException if the file exists but you don't have the 'Contents' permission.
            if (launchMode.isDevOrTest()) {
                LOG.warn("Unable to read file " + fullPath + " for repository " + repository
                        + ". Either the file does not exist or the 'Contents' permission has not been set for the application.");
            }
            return Optional.empty();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Error downloading file " + fullPath + " for repository " + repository, e);
        }
    }
}
