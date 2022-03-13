package io.quarkiverse.githubaction.it;

import org.kohsuke.github.GHEventPayload;

import io.quarkiverse.githubaction.Action;
import io.quarkiverse.githubapp.event.Issue;

public class IssueOpenedAction {

    public static final String ACTION_NAME = "IssueOpenedAction";
    public static final String TEST_OUTPUT = "IssueOpenedAction - Test output";

    @Action(ACTION_NAME)
    void test(@Issue.Opened GHEventPayload.Issue issuePayload) {
        System.out.println(TEST_OUTPUT);
        System.out.println("Repository: " + issuePayload.getIssue().getRepository().getFullName());
        System.out.println("Issue title: " + issuePayload.getIssue().getTitle());
    }
}
