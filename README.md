# Quarkus GitHub Action

[![Version](https://img.shields.io/maven-central/v/io.quarkiverse.githubaction/quarkus-github-action?logo=apache-maven&style=for-the-badge)](https://central.sonatype.com/artifact/io.quarkiverse.githubaction/quarkus-github-action)
<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-2-orange.svg?style=for-the-badge)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->

<p align="center"><img src="https://design.jboss.org/quarkus/bot/final/images/quarkusbot_full.svg" width="128" height="128" /></p>

**Develop your GitHub Actions in Java with Quarkus**

> _Interested in GitHub Apps? Have a look at the [Quarkus GitHub App extension](https://github.com/quarkiverse/quarkus-github-app/)._

Quarkus GitHub Action is a [Quarkus](https://quarkus.io) extension
that allows to create GitHub Actions in Java with very little boilerplate.

And yes, it supports generating native executables with GraalVM or Mandrel.

Your GitHub Action will look like:

```java
class MyAction {

  @Action
  void doSomething() {
    // do something useful here
  }
}
```

Or you can also leverage the GitHub REST API (GraphQL is also supported), get the execution context, get the inputs, execute commands... with something a bit more involved:

```java
class MyAction {

  @Action
  void onIssueOpened(@Issue.Opened GHEventPayload.Issue issuePayload, Context context, Inputs inputs, Commands commands) throws IOException {
    issuePayload.getIssue().comment("Hello from MyAction");

    commands.setOutput("output-key", "the value");
  }
}
```

Quarkus GitHub Action will automatically inject all these fully initialized instances in the `@Action` methods for you.

Focus on your business logic and don't bother about the ceremony.

## Compatibility

Quarkus GitHub App provides two different version streams, one compatible with Quarkus 2.x and the other compatible with Quarkus 3.x.

| Quarkus | Quarkus GitHub Action | Documentation                                                                                        |
|---------|-----------------------|------------------------------------------------------------------------------------------------------|
| 3.x     | 2.x                   | [Documentation](https://docs.quarkiverse.io/quarkus-github-action/dev/index.html) |
| 2.x     | 1.x                   | [Documentation](https://docs.quarkiverse.io/quarkus-github-action/1.x/index.html) |

Use the latest version of the corresponding stream, [the list of versions is available on Maven Central](https://search.maven.org/artifact/io.quarkiverse.githubaction/quarkus-github-action).

## Documentation

To get you started (and more!), please refer to [the extensive documentation](https://quarkiverse.github.io/quarkiverse-docs/quarkus-github-action/dev/index.html) - or for [a version compatible with Quarkus 2.x](https://quarkiverse.github.io/quarkiverse-docs/quarkus-github-action/1.x/index.html).

Anything unclear or missing in the documentation? Please [open an issue](https://github.com/quarkiverse/quarkus-github-action/issues/new).

## How?

The Quarkus GitHub Action extension uses the [Hub4j GitHub API](https://github.com/hub4j/github-api)
to parse the webhook payloads and handle the [GitHub REST API](https://docs.github.com/en/rest) calls.

It can also execute GraphQL queries towards the [GitHub GraphQL API](https://docs.github.com/en/graphql) via the SmallRye GraphQL Client.

The rest of the extension is Quarkus magic - mostly code generation with [Gizmo](https://github.com/quarkusio/gizmo/) -
to get everything wired.

## Status

This extension is considered stable and is used in production.

It relies on the exact same principles as the [Quarkus GitHub App extension](https://github.com/quarkiverse/quarkus-github-app/).

## License

This project is licensed under the [Apache License Version 2.0](./LICENSE.txt).

## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tbody>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://lesincroyableslivres.fr/"><img src="https://avatars.githubusercontent.com/u/1279749?v=4?s=100" width="100px;" alt="Guillaume Smet"/><br /><sub><b>Guillaume Smet</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-github-action/commits?author=gsmet" title="Code">💻</a> <a href="#maintenance-gsmet" title="Maintenance">🚧</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/janpk"><img src="https://avatars.githubusercontent.com/u/809194?v=4?s=100" width="100px;" alt="Jan-Petter Krüger"/><br /><sub><b>Jan-Petter Krüger</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-github-action/commits?author=janpk" title="Code">💻</a></td>
    </tr>
  </tbody>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!
