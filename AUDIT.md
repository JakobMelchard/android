# Public-readiness audit

Open items to settle before switching the repository to public. Values are never listed here.

`gitleaks git` over all refs and `gitleaks dir` over the tree found no secrets.

## History

| Kind | Commits | Path | Action |
|---|---|---|---|
| Personal email as commit author | fb44175 2bf9275 57020fd cff7f72 f8daabb 399306e 262aa68 1d18fda 2eb888a d324b2e 99397c3 694ca08 bea8ee5 bc06456 | commit metadata | Accept, or rewrite with `git filter-repo --mailmap` to the noreply address and force-push before going public. Turn on GitHub email privacy so web merges stop using it. |
| Secret-store paths and a sibling repo name in commit messages | 1d18fda 2eb888a | commit messages | Low risk; accept or reword in the same rewrite. |

## Tree, not changed here

| Kind | Path | Action |
|---|---|---|
| Infisical machine identity id, project slug, env and secret path | `.github/workflows/release.yml` | Not a secret, but it maps the org's secret store. Consider moving them to repo variables. |
| Internal keystore name, secret-store path and a sibling repo in comments | `.github/workflows/release.yml` | Reword the comments. |
| Workflow prints the signing key alias | `.github/workflows/release.yml` | Drop the echo if the alias should stay private. |
| Codespaces access to a private repo (`JakobMelchard/.agents`) | `.devcontainer/devcontainer.json` | Templated from `JakobMelchard/template`; public users cannot use it. Fix in the template or accept. |
| Devcontainer features from `ghcr.io/jakobmelchard/devcontainer` | `.devcontainer/devcontainer.json` | Check the packages are public. |
| Template source is private | `.copier-answers.yml` | `copier update` only works for the owner; accept. |
| README says the repo is private and Obtainium needs a token | `README.md` | Drop the token note when the repo is public. |

## Open

| Item | Action |
|---|---|
| No LICENSE | Owner picks one before going public. |
