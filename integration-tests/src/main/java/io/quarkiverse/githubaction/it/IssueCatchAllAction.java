package io.quarkiverse.githubaction.it;

import org.kohsuke.github.GHEventPayload;

import io.quarkiverse.githubaction.Action;
import io.quarkiverse.githubapp.event.Issue;

public class IssueCatchAllAction {

    public static final String ACTION_NAME = "IssueCatchAllAction";
    public static final String TEST_OUTPUT = "IssueCatchAllAction - Test output";

    @Action(ACTION_NAME)
    void test(@Issue GHEventPayload.Issue issuePayload) {
        System.out.println(TEST_OUTPUT);
        System.out.println("Repository: " + issuePayload.getIssue().getRepository().getFullName());
        System.out.println("Issue title: " + issuePayload.getIssue().getTitle());
    }
}
