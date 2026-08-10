# Release signing

No private key is stored in this repository.

For GitHub Actions, create these repository secrets:
- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

The release workflow decodes the keystore into a temporary runner file and exports the
corresponding environment variables. If the secrets are absent, CI still builds an
unsigned release artifact for technical validation; it must not be uploaded to a store.

Recommended policy:
1. Use a dedicated upload key.
2. Keep passwords only in GitHub Actions secrets.
3. Rotate the upload key according to your release policy.
4. Never commit `.jks`, `.keystore`, private keys, or passwords.
