package io.quarkiverse.githubaction.deployment;

import java.util.Set;

import org.jboss.jandex.DotName;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import io.quarkiverse.githubaction.Action;
import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.ConfigFile;
import io.quarkiverse.githubaction.Context;
import io.quarkiverse.githubaction.Inputs;
import io.quarkiverse.githubaction.runtime.Multiplexer;
import io.quarkiverse.githubapp.event.Event;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;

final class GitHubActionDotNames {

    static final DotName ACTION = DotName.createSimple(Action.class.getName());
    static final DotName EVENT = DotName.createSimple(Event.class.getName());
    static final DotName MULTIPLEXER = DotName.createSimple(Multiplexer.class.getName());
    static final DotName CONFIG_FILE = DotName.createSimple(ConfigFile.class.getName());

    static final DotName GITHUB = DotName.createSimple(GitHub.class.getName());
    static final DotName REPOSITORY = DotName.createSimple(GHRepository.class.getName());
    static final DotName DYNAMIC_GRAPHQL_CLIENT = DotName.createSimple(DynamicGraphQLClient.class.getName());
    static final DotName CONTEXT = DotName.createSimple(Context.class.getName());
    static final DotName INPUTS = DotName.createSimple(Inputs.class.getName());
    static final DotName COMMANDS = DotName.createSimple(Commands.class.getName());

    static final Set<DotName> INJECTABLE_TYPES = Set.of(
            GITHUB, REPOSITORY, DYNAMIC_GRAPHQL_CLIENT, CONTEXT, INPUTS, COMMANDS);

    private GitHubActionDotNames() {
    }
}
