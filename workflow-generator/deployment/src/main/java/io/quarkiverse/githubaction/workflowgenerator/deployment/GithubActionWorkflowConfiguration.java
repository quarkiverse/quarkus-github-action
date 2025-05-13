package io.quarkiverse.githubaction.workflowgenerator.deployment;

import static io.quarkus.runtime.annotations.ConfigPhase.BUILD_TIME;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = BUILD_TIME)
@ConfigMapping(prefix = "quarkus.github-action.workflow")
public interface GithubActionWorkflowConfiguration {

    /**
     * The generated workflow name.
     */
    @WithDefault("build")
    String name();

    /**
     * The generated workflow runner
     */
    @WithDefault("ubuntu-latest")
    String runner();

    /**
     * The generation configuration.
     */
    Generation generation();

    /**
     * The JDK configuration.
     */
    Jdk jdk();

    interface Jdk {

        /**
         * The JDK distribution to use.
         */
        @WithDefault("adopt")
        String distribution();

        /**
         * The JDK version to use.
         */
        Optional<String> version();
    }

    interface Generation {
        /**
         * Whether to enable the github action generation at build time.
         */
        @WithDefault("false")
        boolean enabled();
    }
}
