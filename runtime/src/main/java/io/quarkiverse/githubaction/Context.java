package io.quarkiverse.githubaction;

/**
 * The execution context of the action.
 * <p>
 * Expose all the information provided by GitHub.
 */
public interface Context {

    String getHome();

    String getGitHubJob();

    String getGitHubRef();

    String getGitHubSha();

    String getGitHubRepository();

    String getGitHubRepositoryOwner();

    Long getGitHubRunId();

    Long getGitHubRunNumber();

    Integer getGitHubRetentionDays();

    Integer getGitHubRunAttempt();

    String getGitHubActor();

    String getGitHubWorkflow();

    String getGitHubHeadRef();

    String getGitHubBaseRef();

    String getGitHubEventName();

    String getGitHubServerUrl();

    String getGitHubApiUrl();

    String getGithubGraphQLUrl();

    String getGitHubRefName();

    String getGitHubRefProtected();

    String getGitHubRefType();

    String getGitHubWorkspace();

    String getGitHubAction();

    String getGitHubEventPath();

    String getGitHubActionRepository();

    String getGitHubActionRef();

    String getGitHubPath();

    String getGitHubEnv();

    String getRunnerOs();

    String getRunnerArch();

    String getRunnerName();

    String getRunnerToolCache();

    String getRunnerTemp();

    String getRunnerWorkspace();

    String getActionsRuntimeUrl();

    String getActionsRuntimeToken();

    String getActionsCacheUrl();

    void print();
}
