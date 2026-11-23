# PR-PFLICHT

Pushing and pull requests follow the family rules in the profile repository,
[rules/core/git-and-prs.md](https://github.com/astrapi69/astrapi69/blob/main/rules/core/git-and-prs.md):
AG-060 (push every commit at once), AG-073 (a pull request against `develop` with every push), AG-074 (the cases
without one), AG-075 (one concern per pull request). They are not repeated here, so a copy cannot drift from them.

## What only this repository adds

- **Hotfix branches.** A pull request that patches a release goes against `hotfix/X.Y.Z` instead of `develop` and is
  merged with `make merge-pr PR=n TARGET=hotfix/X.Y.Z`; the script refuses a pull request whose base is not the
  target it was given. The form of a hotfix is in release-workflow.md (#395).
- **The release freeze** is enforced here by the required check `release-freeze` (vibe-coding.md).
- **Closing keywords** follow GITHUB-ISSUE-PFLICHT (github-issue-policy.md).
