package io.quarkiverse.githubaction.runtime;

public interface GitHubEventHandler {

    public void handle(GitHubEvent gitHubEvent);
}
