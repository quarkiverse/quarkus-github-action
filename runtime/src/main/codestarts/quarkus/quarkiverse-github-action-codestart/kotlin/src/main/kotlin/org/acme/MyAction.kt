package org.acme;

import io.quarkiverse.githubaction.Action;
import io.quarkiverse.githubaction.Commands;

class MyAction {

    @Action
    fun action(commands: Commands) {
        commands.notice("Hello from Quarkus GitHub Action");

        commands.appendJobSummary(":wave: Hello from Quarkus GitHub Action");
    }
}
