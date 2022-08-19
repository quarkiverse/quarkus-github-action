package io.quarkiverse.githubaction.runtime;

import io.quarkiverse.githubaction.Context;

class ContextImpl implements Context {

    private final String home;
    private final String gitHubJob;
    private final String gitHubRef;
    private final String gitHubSha;
    private final String gitHubRepository;
    private final String gitHubRepositoryOwner;
    private final String gitHubRunId;
    private final String gitHubRunNumber;
    private final String gitHubRetentionDays;
    private final String gitHubRunAttempt;
    private final String gitHubActor;
    private final String gitHubWorkflow;
    private final String gitHubHeadRef;
    private final String gitHubBaseRef;
    private final String gitHubEventName;
    private final String gitHubServerUrl;
    private final String gitHubApiUrl;
    private final String githubGraphQLUrl;
    private final String gitHubRefName;
    private final String gitHubRefProtected;
    private final String gitHubRefType;
    private final String gitHubWorkspace;
    private final String gitHubAction;
    private final String gitHubEventPath;
    private final String gitHubActionRepository;
    private final String gitHubActionRef;
    private final String gitHubPath;
    private final String gitHubEnv;
    private final String runnerOs;
    private final String runnerArch;
    private final String runnerName;
    private final String runnerToolCache;
    private final String runnerTemp;
    private final String runnerWorkspace;
    private final String actionsRuntimeUrl;
    private final String actionsRuntimeToken;
    private final String actionsCacheUrl;

    ContextImpl() {
        home = System.getenv("HOME");
        gitHubJob = System.getenv("GITHUB_JOB");
        gitHubRef = System.getenv("GITHUB_REF");
        gitHubSha = System.getenv("GITHUB_SHA");
        gitHubRepository = System.getenv("GITHUB_REPOSITORY");
        gitHubRepositoryOwner = System.getenv("GITHUB_REPOSITORY_OWNER");
        gitHubRunId = System.getenv("GITHUB_RUN_ID");
        gitHubRunNumber = System.getenv("GITHUB_RUN_NUMBER");
        gitHubRetentionDays = System.getenv("GITHUB_RETENTION_DAYS");
        gitHubRunAttempt = System.getenv("GITHUB_RUN_ATTEMPT");
        gitHubActor = System.getenv("GITHUB_ACTOR");
        gitHubWorkflow = System.getenv("GITHUB_WORKFLOW");
        gitHubHeadRef = System.getenv("GITHUB_HEAD_REF");
        gitHubBaseRef = System.getenv("GITHUB_BASE_REF");
        gitHubEventName = System.getenv("GITHUB_EVENT_NAME");
        gitHubServerUrl = System.getenv("GITHUB_SERVER_URL");
        gitHubApiUrl = System.getenv("GITHUB_API_URL");
        githubGraphQLUrl = System.getenv("GITHUB_GRAPHQL_URL");
        gitHubRefName = System.getenv("GITHUB_REF_NAME");
        gitHubRefProtected = System.getenv("GITHUB_REF_PROTECTED");
        gitHubRefType = System.getenv("GITHUB_REF_TYPE");
        gitHubWorkspace = System.getenv("GITHUB_WORKSPACE");
        gitHubAction = System.getenv("GITHUB_ACTION");
        gitHubEventPath = System.getenv("GITHUB_EVENT_PATH");
        gitHubActionRepository = System.getenv("GITHUB_ACTION_REPOSITORY");
        gitHubActionRef = System.getenv("GITHUB_ACTION_REF");
        gitHubPath = System.getenv("GITHUB_PATH");
        gitHubEnv = System.getenv("GITHUB_ENV");
        runnerOs = System.getenv("RUNNER_OS");
        runnerArch = System.getenv("RUNNER_ARCH");
        runnerName = System.getenv("RUNNER_NAME");
        runnerToolCache = System.getenv("RUNNER_TOOL_CACHE");
        runnerTemp = System.getenv("RUNNER_TEMP");
        runnerWorkspace = System.getenv("RUNNER_WORKSPACE");
        actionsRuntimeUrl = System.getenv("ACTIONS_RUNTIME_URL");
        actionsRuntimeToken = System.getenv("ACTIONS_RUNTIME_TOKEN");
        actionsCacheUrl = System.getenv("ACTIONS_CACHE_URL");
    }

    @Override
    public String getHome() {
        return home;
    }

    @Override
    public String getGitHubJob() {
        return gitHubJob;
    }

    @Override
    public String getGitHubRef() {
        return gitHubRef;
    }

    @Override
    public String getGitHubSha() {
        return gitHubSha;
    }

    @Override
    public String getGitHubRepository() {
        return gitHubRepository;
    }

    @Override
    public String getGitHubRepositoryOwner() {
        return gitHubRepositoryOwner;
    }

    @Override
    public Long getGitHubRunId() {
        return gitHubRunId != null ? Long.valueOf(gitHubRunId) : null;
    }

    @Override
    public Long getGitHubRunNumber() {
        return gitHubRunNumber != null ? Long.valueOf(gitHubRunNumber) : null;
    }

    @Override
    public String getGitHubRetentionDays() {
        return gitHubRetentionDays;
    }

    @Override
    public String getGitHubRunAttempt() {
        return gitHubRunAttempt;
    }

    @Override
    public String getGitHubActor() {
        return gitHubActor;
    }

    @Override
    public String getGitHubWorkflow() {
        return gitHubWorkflow;
    }

    @Override
    public String getGitHubHeadRef() {
        return gitHubHeadRef;
    }

    @Override
    public String getGitHubBaseRef() {
        return gitHubBaseRef;
    }

    @Override
    public String getGitHubEventName() {
        return gitHubEventName;
    }

    @Override
    public String getGitHubServerUrl() {
        return gitHubServerUrl;
    }

    @Override
    public String getGitHubApiUrl() {
        return gitHubApiUrl;
    }

    @Override
    public String getGithubGraphQLUrl() {
        return githubGraphQLUrl;
    }

    @Override
    public String getGitHubRefName() {
        return gitHubRefName;
    }

    @Override
    public String getGitHubRefProtected() {
        return gitHubRefProtected;
    }

    @Override
    public String getGitHubRefType() {
        return gitHubRefType;
    }

    @Override
    public String getGitHubWorkspace() {
        return gitHubWorkspace;
    }

    @Override
    public String getGitHubAction() {
        return gitHubAction;
    }

    @Override
    public String getGitHubEventPath() {
        return gitHubEventPath;
    }

    @Override
    public String getGitHubActionRepository() {
        return gitHubActionRepository;
    }

    @Override
    public String getGitHubActionRef() {
        return gitHubActionRef;
    }

    @Override
    public String getGitHubPath() {
        return gitHubPath;
    }

    @Override
    public String getGitHubEnv() {
        return gitHubEnv;
    }

    @Override
    public String getRunnerOs() {
        return runnerOs;
    }

    @Override
    public String getRunnerArch() {
        return runnerArch;
    }

    @Override
    public String getRunnerName() {
        return runnerName;
    }

    @Override
    public String getRunnerToolCache() {
        return runnerToolCache;
    }

    @Override
    public String getRunnerTemp() {
        return runnerTemp;
    }

    @Override
    public String getRunnerWorkspace() {
        return runnerWorkspace;
    }

    @Override
    public String getActionsRuntimeUrl() {
        return actionsRuntimeUrl;
    }

    @Override
    public String getActionsRuntimeToken() {
        return actionsRuntimeToken;
    }

    @Override
    public String getActionsCacheUrl() {
        return actionsCacheUrl;
    }

    @Override
    public void print() {
        System.out.println("========= Context =========");
        System.out.println("home: " + home);
        System.out.println("gitHubJob: " + gitHubJob);
        System.out.println("gitHubRef: " + gitHubRef);
        System.out.println("gitHubSha: " + gitHubSha);
        System.out.println("gitHubRepository: " + gitHubRepository);
        System.out.println("gitHubRepositoryOwner: " + gitHubRepositoryOwner);
        System.out.println("gitHubRunId: " + gitHubRunId);
        System.out.println("gitHubRunNumber: " + gitHubRunNumber);
        System.out.println("gitHubRetentionDays: " + gitHubRetentionDays);
        System.out.println("gitHubRunAttempt: " + gitHubRunAttempt);
        System.out.println("gitHubActor: " + gitHubActor);
        System.out.println("gitHubWorkflow: " + gitHubWorkflow);
        System.out.println("gitHubHeadRef: " + gitHubHeadRef);
        System.out.println("gitHubBaseRef: " + gitHubBaseRef);
        System.out.println("gitHubEventName: " + gitHubEventName);
        System.out.println("gitHubServerUrl: " + gitHubServerUrl);
        System.out.println("gitHubApiUrl: " + gitHubApiUrl);
        System.out.println("githubGraphQLUrl: " + githubGraphQLUrl);
        System.out.println("gitHubRefName: " + gitHubRefName);
        System.out.println("gitHubRefProtected: " + gitHubRefProtected);
        System.out.println("gitHubRefType: " + gitHubRefType);
        System.out.println("gitHubWorkspace: " + gitHubWorkspace);
        System.out.println("gitHubAction: " + gitHubAction);
        System.out.println("gitHubEventPath: " + gitHubEventPath);
        System.out.println("gitHubActionRepository: " + gitHubActionRepository);
        System.out.println("gitHubActionRef: " + gitHubActionRef);
        System.out.println("gitHubPath: " + gitHubPath);
        System.out.println("gitHubEnv: " + gitHubEnv);
        System.out.println("runnerOs: " + runnerOs);
        System.out.println("runnerArch: " + runnerArch);
        System.out.println("runnerName: " + runnerName);
        System.out.println("runnerToolCache: " + runnerToolCache);
        System.out.println("runnerTemp: " + runnerTemp);
        System.out.println("runnerWorkspace: " + runnerWorkspace);
        System.out.println("actionsRuntimeUrl: " + actionsRuntimeUrl);
        System.out.println("actionsRuntimeToken: " + actionsRuntimeToken);
        System.out.println("actionsCacheUrl: " + actionsCacheUrl);
        System.out.println("===========================");
    }
}
