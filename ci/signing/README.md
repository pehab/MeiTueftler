# Private CI test signing

Set the repository Actions secret `MEITUEFTLER_TEST_KEYSTORE_BASE64` to the Base64-encoded bytes of a **private test** keystore. The workflow decodes it into the runner's temporary directory, never uploads it as an artifact, and passes its path to Gradle.

Expected alias: `androiddebugkey`; store/key password: `android` (this password is public; the actual keystore must remain private). Keep this key separate from production/Play signing keys.

Example creation on a trusted local computer:

```bash
keytool -genkeypair -keystore meitueftler-test.keystore -storetype PKCS12 -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=MeiTueftler Test Key"
```

Base64-encode the resulting file and paste it directly into the GitHub Actions secret. Keep a private backup. Never commit or publish the file or its Base64 value. The repository ignores `*.keystore` and `*.jks` files.

The first test builds used ephemeral Android-generated keys. Moving from those builds to a fixed certificate requires one uninstall/reinstall and removes local app data. Without the secret CI continues producing playable test APKs with ephemeral signatures.
