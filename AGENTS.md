# pull

Use the Makefile with JDK 17. Never install on a device without explicit owner approval.
Preserve the application ID and signing identity at the repository root. Signing inputs
stay in the secret store; never print or commit them. Publish only verified increasing
versions with `scripts/publish-apk`.
