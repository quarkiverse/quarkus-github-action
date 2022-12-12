{#if project.name}
# {project.name}
{#else}
# {project.artifact}
{/if}

{#if project.description}
> {project.description}

{/if}

This repository contains a GitHub Action powered by https://github.com/quarkiverse/quarkus-github-action[Quarkus GitHub Action].

When pushing to the `main` branch, the GitHub Action artifact will automatically be published to the repository's Maven repository hosted on GitHub.

The `action.yml` file runs this published artifact using JBang when the action is executed.
