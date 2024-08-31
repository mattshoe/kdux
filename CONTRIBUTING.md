# Contributing to Kdux

First off, thanks for taking the time to contribute!

The following is a set of guidelines for contributing to Kdux, a library to simplify KSP development. 
These are mostly guidelines, not rules. Use your best judgment, and feel free to propose changes to this document in
a pull request.

## How Can I Contribute?

### Picking up Issues
Anyone is more than welcome to create or pick up any issues they see fit!

Any [Issues](https://github.com/mattshoe/kdux/issues) that are assigned to [@mattshoe](https://github.com/mattshoe) are 
generally just defaulted that way, so you are welcome to assign them to yourself instead. If that is problematic then they
will reach out to you and let you know. Just make sure you assign it to yourself BEFORE you start working on it to let him
know!

### [Kdux Task Board](https://github.com/users/mattshoe/projects/3)
The Kdux project has a public KanBan style [task board](https://github.com/users/mattshoe/projects/3) used to track active work.

#### You are more than welcome to:
- Create your own tasks
- Pick up tasks
  - Make sure you move them to `In-Progress`/`Ready for Review`/`Done` 
- Comment on tasks
- It's public, so use your best judgment and be a good citizen!

### Reporting Bugs

This section guides you through submitting a bug report for Kdux. Following these guidelines helps maintainers and 
the community understand your report, reproduce the behavior, and find related reports.

- **Use a clear and descriptive title** for the issue to identify the problem.
- **Describe the exact steps to reproduce the problem** with as much detail as possible.
- **Provide specific examples to demonstrate the steps**.
- **Describe the behavior you observed after following the steps** and explain why it is a problem.
- **Explain which behavior you expected to see instead and why**.

### Suggesting Enhancements

This section guides you through submitting an enhancement suggestion for Kdux, including completely new features and minor improvements to existing functionality.

- **Use a clear and descriptive title** for the issue to identify the suggestion.
- **Provide a step-by-step description of the suggested enhancement** in as much detail as possible.
- **Provide specific examples for the problem the enhancement solves, or the advantage it provides**.
- **Describe the current behavior** and **explain how the enhancement improves this**.

## Pull Requests
Please follow these steps to have your contribution considered by the maintainers:

1. **Fork the repository** and create a `feature/XXX` branch from `develop`.
2. **If you've added code** that should be tested, add tests.
3. **If you've changed APIs**, update the documentation.
   - This includes README, KDocs, and any other associated documentation.
4. **Ensure the test suite passes**.
5. **Open a pull request**.
6. **Tag the Title** with the [Issue](https://github.com/mattshoe/kdux/issues)# the PR is associated with
   - PRs should usually be associated with a [task board](https://github.com/users/mattshoe/projects/2) issue

## Style Guide

### Coding Standards

- Follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) for all Kotlin code.
- Ensure that your code is well-tested.
- Write clear and meaningful commit messages.

### Commit Message Guidelines

- **Use the present tense** ("Add feature" not "Added feature").
- **Use the imperative mood** ("Move cursor to..." not "Moves cursor to...").
- **Limit the first line to 72 characters or less**.
- **Reference issues and pull requests liberally** after the first line.

# Release Distribution
Releases are (mostly) automated with [GitHub Actions](https://github.com/mattshoe/kdux/actions).<br>
See [publish_release.yaml](.github/workflows/publish_release.yaml) <br>
See [Publish Release Job](https://github.com/mattshoe/kdux/actions/workflows/publish_release.yaml)

### Summary
Every time the `version` property in `gradle.properties` is changed and pushed to `main`, that commit will be tagged with
the new version, and a new [GitHub Release](https://github.com/mattshoe/kdux/releases) will be generated with all 
relevant artifacts attached as assets to the [GitHub Release](https://github.com/mattshoe/kdux/releases).

#### GitHub Release Artifacts include:
1. The zipped artifacts for the `Kdux.Annotations` module
2. The zipped artifacts for the `Kdux.Processor` module
3. The zipped artifacts for the `Kdux.Runtime` module


#### Maven Central Publication
The [Publish Release Action](https://github.com/mattshoe/kdux/actions/workflows/publish_release.yaml) is configured to
publish release artifacts directly to Nexus for staging on OSSRH. Each publication will require manual validation and 
submission to Maven Central via the Nexus Repository manager. Currently only the repository owner [@mattshoe](https://github.com/mattshoe)
has the permissions for this.


## Additional Notes

### Issue and Pull Request Labels

This section lists the labels we use to help us track and manage issues and pull requests.

- `TODO`: Planned tasks on the [Kdux Task Board](https://github.com/users/mattshoe/projects/2)
- `beginner`: Simple issues which should only require a few lines of code.
- `help-wanted`: Issues which should be a bit more involved than beginner issues.
- `bug`: An issue that reports a bug.
- `enhancement`: An issue that reports an enhancement.

Thank you for contributing to Kdux!