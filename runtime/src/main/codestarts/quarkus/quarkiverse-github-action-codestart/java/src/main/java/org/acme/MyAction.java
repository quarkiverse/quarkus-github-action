package org.acme;

import io.quarkiverse.githubaction.Action;
import io.quarkiverse.githubaction.Commands;

public class MyAction {

    @Action
    void action(Commands commands) {
        commands.notice("Hello from Quarkus GitHub Action");

        commands.appendJobSummary(":wave: Hello from Quarkus GitHub Action");
    }
}