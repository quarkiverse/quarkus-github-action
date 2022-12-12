{#if project.name}
# {project.name}
{#else}
# {project.artifact-id}
{/if}
{#if project.description}

> {project.description}
{/if}

This repository contains a GitHub Action powered by [Quarkus GitHub Action](https://github.com/quarkiverse/quarkus-github-action).

When pushing to the `main` branch, the GitHub Action artifact is automatically published to the Maven repository of this GitHub repository.

The `action.yml` descriptor instructs GitHub Actions to run this published artifact using JBang when the action is executed.
