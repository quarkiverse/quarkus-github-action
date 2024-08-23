package io.quarkiverse.githubaction.runtime;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.Context;
import io.quarkiverse.githubaction.Inputs;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClientBuilder;

@SuppressWarnings("unused")
public class GitHubEvent {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String AUTHORIZATION_HEADER_BEARER = "Bearer %s";

    private final String name;

    private final Context context;
    private final String eventAction;
    private final Inputs inputs;
    private final Commands commands;

    private final Class<? extends GHEventPayload> payloadType;
    private volatile GHEventPayload payload;

    private volatile GitHub gitHubClient;
    private volatile DynamicGraphQLClient gitHubGraphQLClient;
    private volatile GHRepository gitHubRepository;

    GitHubEvent(String name, Context context, String eventAction,
            Inputs inputs, Commands commands,
            Class<? extends GHEventPayload> payloadType) {
        this.name = name;
        this.context = context;
        this.eventAction = eventAction;
        this.inputs = inputs;
        this.commands = commands;
        this.payloadType = payloadType;
    }

    public String getName() {
        return name;
    }

    public String getEvent() {
        return context.getGitHubEventName();
    }

    public String getEventAction() {
        return eventAction;
    }

    public Context getContext() {
        return context;
    }

    public Inputs getInputs() {
        return inputs;
    }

    public Commands getCommands() {
        return commands;
    }

    public GHEventPayload getPayload() {
        GHEventPayload localPayload = this.payload;
        if (localPayload == null) {
            synchronized (this) {
                localPayload = this.payload;
                if (localPayload == null) {
                    try (Reader payloadReader = Files.newBufferedReader(Path.of(context.getGitHubEventPath()))) {
                        this.payload = localPayload = getGitHub().parseEventPayload(payloadReader, payloadType);
                    } catch (IOException e) {
                        throw new IllegalStateException("Unable to read or parse payload file " + context.getGitHubEventPath(),
                                e);
                    }
                }
            }
        }

        return localPayload;
    }

    public GitHub getGitHub() {
        GitHub localGitHubClient = this.gitHubClient;

        if (localGitHubClient == null) {
            synchronized (this) {
                localGitHubClient = this.gitHubClient;
                if (localGitHubClient == null) {
                    try {
                        if (inputs.getGitHubToken().isPresent()) {
                            localGitHubClient = new GitHubBuilder()
                                    .withEndpoint(context.getGitHubApiUrl())
                                    .withAppInstallationToken(inputs.getGitHubToken().get())
                                    .build();

                            // this call is not counted in the rate limit
                            localGitHubClient.getRateLimit();
                        } else {
                            localGitHubClient = GitHub.offline();
                        }

                        this.gitHubClient = localGitHubClient;
                    } catch (Exception e) {
                        throw new IllegalStateException("Unable to initialize the GitHub API client, is the token valid?", e);
                    }
                }
            }
        }

        return localGitHubClient;
    }

    public DynamicGraphQLClient getDynamicGraphQLClient() {
        if (inputs.getGitHubToken().isEmpty()) {
            throw new IllegalStateException("No GitHub token provided, unable to initialize the GitHub GraphQL client");
        }

        DynamicGraphQLClient localGitHubGraphQLClient = this.gitHubGraphQLClient;
        if (localGitHubGraphQLClient == null) {
            synchronized (this) {
                localGitHubGraphQLClient = this.gitHubGraphQLClient;
                if (localGitHubGraphQLClient == null) {
                    try {
                        this.gitHubGraphQLClient = localGitHubGraphQLClient = DynamicGraphQLClientBuilder.newBuilder()
                                .url(context.getGithubGraphQLUrl())
                                .header(AUTHORIZATION_HEADER,
                                        String.format(AUTHORIZATION_HEADER_BEARER, inputs.getGitHubToken().get()))
                                .build();

                        // this call is probably - it's not documented - not counted in the rate limit
                        localGitHubGraphQLClient.executeSync("""
                                query {
                                  rateLimit {
                                    limit
                                    cost
                                    remaining
                                    resetAt
                                  }
                                }""");
                    } catch (Exception e) {
                        throw new IllegalStateException("Unable to initialize the GitHub GraphQL client, is the token valid?",
                                e);
                    }
                }
            }
        }

        return localGitHubGraphQLClient;
    }

    public GHRepository getGHRepository() {
        GHRepository localGitHubRepository = gitHubRepository;
        if (localGitHubRepository == null) {
            synchronized (this) {
                if (gitHubRepository == null) {
                    gitHubRepository = localGitHubRepository = createGHRepository(getGitHub(), context.getGitHubRepository());
                }
            }
        }
        return localGitHubRepository;
    }

    private static GHRepository createGHRepository(GitHub gitHub, String repositoryName) {
        if (gitHub.isOffline()) {
            throw new IllegalStateException(
                    "The GitHub REST client is offline, unable to inject the repository '%s'".formatted(repositoryName));
        }

        try {
            return gitHub.getRepository(repositoryName);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to access repository '%s'".formatted(repositoryName), ex);
        }
    }
}
