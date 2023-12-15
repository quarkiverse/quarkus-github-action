package io.quarkiverse.githubaction.runtime;

import io.quarkiverse.githubaction.Context;

class ContextImpl implements Context {

    private final String home;
    private final String gitHubAction;
    private final String gitHubActionPath;
    private final String gitHubActionRef;
    private final String gitHubActionRepository;
    private final String gitHubActor;
    private final String gitHubActorId;
    private final String gitHubApiUrl;
    private final String gitHubBaseRef;
    private final String gitHubEnv;
    private final String gitHubEventName;
    private final String gitHubEventPath;
    private final String githubGraphQLUrl;
    private final String gitHubHeadRef;
    private final String gitHubJob;
    private final String gitHubOutput;
    private final String gitHubPath;
    private final String gitHubRef;
    private final String gitHubRefName;
    private final String gitHubRefProtected;
    private final String gitHubRefType;
    private final String gitHubRepository;
    private final String gitHubRepositoryId;
    private final String gitHubRepositoryOwner;
    private final String gitHubRepositoryOwnerId;
    private final String gitHubRetentionDays;
    private final String gitHubRunAttempt;
    private final String gitHubRunId;
    private final String gitHubRunNumber;
    private final String gitHubServerUrl;
    private final String gitHubSha;
    private final String gitHubStepSummary;
    private final String gitHubTriggeringActor;
    private final String gitHubWorkflow;
    private final String gitHubWorkflowRef;
    private final String gitHubWorkflowSha;
    private final String gitHubWorkspace;
    private final String runnerArch;
    private final String runnerDebug;
    private final String runnerName;
    private final String runnerOs;
    private final String runnerTemp;
    private final String runnerToolCache;
    private final String runnerWorkspace;
    private final String actionsCacheUrl;
    private final String actionsRuntimeToken;
    private final String actionsRuntimeUrl;

    ContextImpl() {
        home = System.getenv("HOME");
        gitHubActionPath = System.getenv("GITHUB_ACTION_PATH");
        gitHubActionRef = System.getenv("GITHUB_ACTION_REF");
        gitHubActionRepository = System.getenv("GITHUB_ACTION_REPOSITORY");
        gitHubAction = System.getenv("GITHUB_ACTION");
        gitHubActor = System.getenv("GITHUB_ACTOR");
        gitHubActorId = System.getenv("GITHUB_ACTOR_ID");
        gitHubApiUrl = System.getenv("GITHUB_API_URL");
        gitHubBaseRef = System.getenv("GITHUB_BASE_REF");
        gitHubEnv = System.getenv("GITHUB_ENV");
        gitHubEventName = System.getenv("GITHUB_EVENT_NAME");
        gitHubEventPath = System.getenv("GITHUB_EVENT_PATH");
        githubGraphQLUrl = System.getenv("GITHUB_GRAPHQL_URL");
        gitHubHeadRef = System.getenv("GITHUB_HEAD_REF");
        gitHubJob = System.getenv("GITHUB_JOB");
        gitHubOutput = System.getenv("GITHUB_OUTPUT");
        gitHubPath = System.getenv("GITHUB_PATH");
        gitHubRefName = System.getenv("GITHUB_REF_NAME");
        gitHubRefProtected = System.getenv("GITHUB_REF_PROTECTED");
        gitHubRef = System.getenv("GITHUB_REF");
        gitHubRefType = System.getenv("GITHUB_REF_TYPE");
        gitHubRepository = System.getenv("GITHUB_REPOSITORY");
        gitHubRepositoryId = System.getenv("GITHUB_REPOSITORY_ID");
        gitHubRepositoryOwner = System.getenv("GITHUB_REPOSITORY_OWNER");
        gitHubRepositoryOwnerId = System.getenv("GITHUB_REPOSITORY_OWNER_ID");
        gitHubRetentionDays = System.getenv("GITHUB_RETENTION_DAYS");
        gitHubRunAttempt = System.getenv("GITHUB_RUN_ATTEMPT");
        gitHubRunId = System.getenv("GITHUB_RUN_ID");
        gitHubRunNumber = System.getenv("GITHUB_RUN_NUMBER");
        gitHubServerUrl = System.getenv("GITHUB_SERVER_URL");
        gitHubSha = System.getenv("GITHUB_SHA");
        gitHubStepSummary = System.getenv("GITHUB_STEP_SUMMARY");
        gitHubTriggeringActor = System.getenv("GITHUB_TRIGGERING_ACTOR");
        gitHubWorkflow = System.getenv("GITHUB_WORKFLOW");
        gitHubWorkflowRef = System.getenv("GITHUB_WORKFLOW_REF");
        gitHubWorkflowSha = System.getenv("GITHUB_WORKFLOW_SHA");
        gitHubWorkspace = System.getenv("GITHUB_WORKSPACE");
        runnerArch = System.getenv("RUNNER_ARCH");
        runnerDebug = System.getenv("RUNNER_DEBUG");
        runnerName = System.getenv("RUNNER_NAME");
        runnerOs = System.getenv("RUNNER_OS");
        runnerTemp = System.getenv("RUNNER_TEMP");
        runnerToolCache = System.getenv("RUNNER_TOOL_CACHE");
        runnerWorkspace = System.getenv("RUNNER_WORKSPACE");
        actionsCacheUrl = System.getenv("ACTIONS_CACHE_URL");
        actionsRuntimeToken = System.getenv("ACTIONS_RUNTIME_TOKEN");
        actionsRuntimeUrl = System.getenv("ACTIONS_RUNTIME_URL");
    }

    @Override
    public String getHome() {
        return home;
    }

    @Override
    public String getGitHubAction() {
        return gitHubAction;
    }

    @Override
    public String getGitHubActionPath() {
        return gitHubActionPath;
    }

    @Override
    public String getGitHubActionRef() {
        return gitHubActionRef;
    }

    @Override
    public String getGitHubActionRepository() {
        return gitHubActionRepository;
    }

    @Override
    public String getGitHubActor() {
        return gitHubActor;
    }

    @Override
    public Long getGitHubActorId() {
        return gitHubActorId != null && !gitHubActorId.isBlank() ? Long.valueOf(gitHubActorId) : null;
    }

    @Override
    public String getGitHubApiUrl() {
        return gitHubApiUrl;
    }

    @Override
    public String getGitHubBaseRef() {
        return gitHubBaseRef;
    }

    @Override
    public String getGitHubEnv() {
        return gitHubEnv;
    }

    @Override
    public String getGitHubEventName() {
        return gitHubEventName;
    }

    @Override
    public String getGitHubEventPath() {
        return gitHubEventPath;
    }

    @Override
    public String getGithubGraphQLUrl() {
        return githubGraphQLUrl;
    }

    @Override
    public String getGitHubHeadRef() {
        return gitHubHeadRef;
    }

    @Override
    public String getGitHubJob() {
        return gitHubJob;
    }

    @Override
    public String getGitHubOutput() {
        return gitHubOutput;
    }

    @Override
    public String getGitHubPath() {
        return gitHubPath;
    }

    @Override
    public String getGitHubRef() {
        return gitHubRef;
    }

    @Override
    public String getGitHubRefName() {
        return gitHubRefName;
    }

    @Override
    public boolean isGitHubRefProtected() {
        return "true".equalsIgnoreCase(gitHubRefProtected);
    }

    @Override
    public String getGitHubRefType() {
        return gitHubRefType;
    }

    @Override
    public String getGitHubRepository() {
        return gitHubRepository;
    }

    @Override
    public Long getGitHubRepositoryId() {
        return gitHubRepositoryId != null && !gitHubRepositoryId.isBlank() ? Long.valueOf(gitHubRepositoryId) : null;
    }

    @Override
    public String getGitHubRepositoryOwner() {
        return gitHubRepositoryOwner;
    }

    @Override
    public Long getGitHubRepositoryOwnerId() {
        return gitHubRepositoryOwnerId != null && !gitHubRepositoryOwnerId.isBlank() ? Long.valueOf(gitHubRepositoryOwnerId)
                : null;
    }

    @Override
    public Integer getGitHubRetentionDays() {
        return gitHubRetentionDays != null && !gitHubRetentionDays.isBlank() ? Integer.valueOf(gitHubRetentionDays) : null;
    }

    @Override
    public Integer getGitHubRunAttempt() {
        return gitHubRunAttempt != null && !gitHubRunAttempt.isBlank() ? Integer.valueOf(gitHubRunAttempt) : null;
    }

    @Override
    public Long getGitHubRunId() {
        return gitHubRunId != null && !gitHubRunId.isBlank() ? Long.valueOf(gitHubRunId) : null;
    }

    @Override
    public Long getGitHubRunNumber() {
        return gitHubRunNumber != null && !gitHubRunNumber.isBlank() ? Long.valueOf(gitHubRunNumber) : null;
    }

    @Override
    public String getGitHubServerUrl() {
        return gitHubServerUrl;
    }

    @Override
    public String getGitHubSha() {
        return gitHubSha;
    }

    @Override
    public String getGitHubStepSummary() {
        return gitHubStepSummary;
    }

    @Override
    public String getGitHubTriggeringActor() {
        return gitHubTriggeringActor;
    }

    @Override
    public String getGitHubWorkflow() {
        return gitHubWorkflow;
    }

    @Override
    public String getGitHubWorkflowRef() {
        return gitHubWorkflowRef;
    }

    @Override
    public String getGitHubWorkflowSha() {
        return gitHubWorkflowSha;
    }

    @Override
    public String getGitHubWorkspace() {
        return gitHubWorkspace;
    }

    @Override
    public String getRunnerArch() {
        return runnerArch;
    }

    @Override
    public String getRunnerDebug() {
        return runnerDebug;
    }

    @Override
    public String getRunnerName() {
        return runnerName;
    }

    @Override
    public String getRunnerOs() {
        return runnerOs;
    }

    @Override
    public String getRunnerTemp() {
        return runnerTemp;
    }

    @Override
    public String getRunnerToolCache() {
        return runnerToolCache;
    }

    @Override
    public String getRunnerWorkspace() {
        return runnerWorkspace;
    }

    @Override
    public String getActionsCacheUrl() {
        return actionsCacheUrl;
    }

    @Override
    public String getActionsRuntimeToken() {
        return actionsRuntimeToken;
    }

    @Override
    public String getActionsRuntimeUrl() {
        return actionsRuntimeUrl;
    }

    @Override
    public void print() {
        System.out.println("========= Context =========");
        System.out.println("home: " + home);
        System.out.println("gitHubAction: " + gitHubAction);
        System.out.println("gitHubActionPath: " + gitHubActionPath);
        System.out.println("gitHubActionRef: " + gitHubActionRef);
        System.out.println("gitHubActionRepository: " + gitHubActionRepository);
        System.out.println("gitHubActor: " + gitHubActor);
        System.out.println("gitHubActorId: " + gitHubActorId);
        System.out.println("gitHubApiUrl: " + gitHubApiUrl);
        System.out.println("gitHubBaseRef: " + gitHubBaseRef);
        System.out.println("gitHubEnv: " + gitHubEnv);
        System.out.println("gitHubEventName: " + gitHubEventName);
        System.out.println("gitHubEventPath: " + gitHubEventPath);
        System.out.println("githubGraphQLUrl: " + githubGraphQLUrl);
        System.out.println("gitHubHeadRef: " + gitHubHeadRef);
        System.out.println("gitHubJob: " + gitHubJob);
        System.out.println("gitHubOutput: " + gitHubOutput);
        System.out.println("gitHubPath: " + gitHubPath);
        System.out.println("gitHubRef: " + gitHubRef);
        System.out.println("gitHubRefName: " + gitHubRefName);
        System.out.println("gitHubRefProtected: " + gitHubRefProtected);
        System.out.println("gitHubRefType: " + gitHubRefType);
        System.out.println("gitHubRepository: " + gitHubRepository);
        System.out.println("gitHubRepositoryId: " + gitHubRepositoryId);
        System.out.println("gitHubRepositoryOwner: " + gitHubRepositoryOwner);
        System.out.println("gitHubRepositoryOwnerId: " + gitHubRepositoryOwnerId);
        System.out.println("gitHubRetentionDays: " + gitHubRetentionDays);
        System.out.println("gitHubRunAttempt: " + gitHubRunAttempt);
        System.out.println("gitHubRunId: " + gitHubRunId);
        System.out.println("gitHubRunNumber: " + gitHubRunNumber);
        System.out.println("gitHubServerUrl: " + gitHubServerUrl);
        System.out.println("gitHubSha: " + gitHubSha);
        System.out.println("gitHubStepSummary: " + gitHubStepSummary);
        System.out.println("gitHubTriggeringActor: " + gitHubTriggeringActor);
        System.out.println("gitHubWorkflow: " + gitHubWorkflow);
        System.out.println("gitHubWorkflowRef: " + gitHubWorkflowRef);
        System.out.println("gitHubWorkflowSha: " + gitHubWorkflowSha);
        System.out.println("gitHubWorkspace: " + gitHubWorkspace);
        System.out.println("runnerArch: " + runnerArch);
        System.out.println("runnerDebug: " + runnerDebug);
        System.out.println("runnerName: " + runnerName);
        System.out.println("runnerOs: " + runnerOs);
        System.out.println("runnerTemp: " + runnerTemp);
        System.out.println("runnerToolCache: " + runnerToolCache);
        System.out.println("runnerWorkspace: " + runnerWorkspace);
        System.out.println("actionsCacheUrl: " + actionsCacheUrl);
        System.out.println("actionsRuntimeToken: " + actionsRuntimeToken);
        System.out.println("actionsRuntimeUrl: " + actionsRuntimeUrl);
        System.out.println("===========================");
    }
}
