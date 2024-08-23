package io.quarkiverse.githubaction.it;

import org.kohsuke.github.GHRepository;

import io.quarkiverse.githubaction.Action;

public class RepositoryAction {
    public static final String ACTION_NAME = "RepositoryAction";

    @Action(ACTION_NAME)
    void test(GHRepository repository) {
        System.out.println(repository.getFullName());
    }
}
