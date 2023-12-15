package io.quarkiverse.githubaction;

/**
 * Execute GitHub Actions commands by printing the command to the output.
 *
 * @see <a href="https://docs.github.com/en/actions/learn-github-actions/workflow-commands-for-github-actions">Workflow
 *      commands for GitHub Actions</a>
 */
public interface Commands {

    /**
     * Sets an action's output parameter.
     */
    void setOutput(String name, String value);

    /**
     * Prints a debug message if a secret named ACTIONS_STEP_DEBUG exists and is set to true.
     *
     * @see <a href=
     *      "https://docs.github.com/en/actions/monitoring-and-troubleshooting-workflows/enabling-debug-logging">Enabling debug
     *      logging</a>
     */
    void debug(String message);

    /**
     * Creates a notice message and prints the message to the log. This message will create an annotation, which can associate
     * the message with a particular file in your repository. Optionally, your message can specify a position within the file.
     */
    void notice(String message);

    /**
     * Creates a notice message and prints the message to the log. This message will create an annotation, which can associate
     * the message with a particular file in your repository. Optionally, your message can specify a position within the file.
     */
    void notice(String message, String title, String file, Integer line, Integer endLine, Integer col, Integer endColumn);

    /**
     * Creates a warning message and prints the message to the log. This message will create an annotation, which can associate
     * the message with a particular file in your repository. Optionally, your message can specify a position within the file.
     */
    void warning(String message);

    /**
     * Creates a warning message and prints the message to the log. This message will create an annotation, which can associate
     * the message with a particular file in your repository. Optionally, your message can specify a position within the file.
     */
    void warning(String message, String title, String file, Integer line, Integer endLine, Integer col, Integer endColumn);

    /**
     * Creates an error message and prints the message to the log. This message will create an annotation, which can associate
     * the message with a particular file in your repository. Optionally, your message can specify a position within the file.
     */
    void error(String message);

    /**
     * Creates an error message and prints the message to the log. This message will create an annotation, which can associate
     * the message with a particular file in your repository. Optionally, your message can specify a position within the file.
     */
    void error(String message, String title, String file, Integer line, Integer endLine, Integer col, Integer endColumn);

    /**
     * Creates an expandable group in the log. To create a group, use the group command and specify a title. Anything you print
     * to the log between the group and endgroup commands is nested inside an expandable entry in the log.
     */
    void group(String title);

    /**
     * Ends an expandable group.
     */
    void endGroup();

    /**
     * Prints the message.
     */
    void echo(String message);

    /**
     * Masking a value prevents a string or variable from being printed in the log. Each masked word separated by whitespace is
     * replaced with the * character. You can use an environment variable or string for the mask's value. When you mask a value,
     * it is treated as a secret and will be redacted on the runner. For example, after you mask a value, you won't be able to
     * set that value as an output.
     */
    void addMask(String value);

    /**
     * Stops processing any workflow commands. This special command allows you to log anything without accidentally running a
     * workflow command. For example, you could stop logging to output an entire script that has comments.
     */
    void stopCommands();

    /**
     * Pursue processing of workflow commands.
     */
    void pursueCommands();

    /**
     * Enables echoing of workflow commands. For example, if you use the set-output command in a workflow, it sets
     * an output parameter but the workflow run's log does not show the command itself. If you enable command echoing, then the
     * log shows the command, such as ::set-output name={name}::{value}.
     */
    void echoOn();

    /**
     * Disables echoing of workflow commands.
     */
    void echoOff();

    /**
     * You can use the save-state command to create environment variables for sharing with your workflow's pre: or post:
     * actions.
     *
     * @see <a href=
     *      "https://docs.github.com/en/actions/using-workflows/workflow-commands-for-github-actions#sending-values-to-the-pre-and-post-actions">Sending
     *      values to the pre and post actions</a>
     */
    void saveState(String name, String value);

    /**
     * Makes an environment variable available to any subsequent steps. The step that creates or updates the environment
     * variable does not have access to the new value, but all subsequent steps in a job will have access.
     *
     * @see <a href="https://docs.github.com/en/actions/learn-github-actions/environment-variables">Environment variables</a>
     */
    void environmentVariable(String name, String value);

    /**
     * Display a job summary on the job page.
     *
     * @see <a href=
     *      "https://docs.github.com/en/actions/using-workflows/workflow-commands-for-github-actions#adding-a-job-summary">Adding
     *      a job summary</a>
     */
    void jobSummary(String markdownContent);

    /**
     * Append to the job summary.
     *
     * @see <a href=
     *      "https://docs.github.com/en/actions/using-workflows/workflow-commands-for-github-actions#adding-a-job-summary">Adding
     *      a job summary</a>
     */
    void appendJobSummary(String markdownContent);

    /**
     * Remove the job summary.
     */
    void removeJobSummary();

    /**
     * Prepends a directory to the system PATH variable and automatically makes it available to all subsequent actions in the
     * current job; the currently running action cannot access the updated path variable. To see the currently defined paths for
     * your job, you can use echo "$PATH" in a step or an action.
     */
    void systemPath(String path);
}
