package io.quarkiverse.githubaction.cli;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.quarkiverse.githubaction.workflowgenerator.spi.GeneratedGithubActionWorkflowResourceBuildItem;
import io.quarkus.builder.BuildResult;

public class GithubActionWorkflowHandler implements BiConsumer<Object, BuildResult> {

    @Override
    public void accept(Object context, BuildResult buildResult) {
        List<GeneratedGithubActionWorkflowResourceBuildItem> workflows = buildResult
                .consumeMulti(GeneratedGithubActionWorkflowResourceBuildItem.class);
        Consumer<List<GeneratedGithubActionWorkflowResourceBuildItem>> consumer = (Consumer<List<GeneratedGithubActionWorkflowResourceBuildItem>>) context;
        consumer.accept(workflows);
    }
}
