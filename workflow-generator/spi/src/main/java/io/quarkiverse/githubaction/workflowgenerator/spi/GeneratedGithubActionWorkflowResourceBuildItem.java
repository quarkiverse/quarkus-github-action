package io.quarkiverse.githubaction.workflowgenerator.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class GeneratedGithubActionWorkflowResourceBuildItem extends MultiBuildItem {

    private final String name;
    private final String content;

    public GeneratedGithubActionWorkflowResourceBuildItem(String name, String content) {
        this.name = name;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }
}
