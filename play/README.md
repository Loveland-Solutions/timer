# Publishing Tile Timer

Everything for Play Console is in this folder. The app id is `mn.lls.tiletimer`.
It cannot change after the first upload.

## Before uploading

1. Back up `keystore/upload.jks` and `keystore.properties` somewhere other than this laptop.
2. Fill in `[CONTACT EMAIL]` in `privacy-policy.md` and host it.
3. Put the hosted URL in `declarations.md`, and add it to the app's About line.
4. Record the demo video on a phone (tile tap, ring, Stop), upload it, and add the link.

## In Play Console

1. Create the app under the Loveland Solutions account: App, Free, en-US.
2. Complete App content from `declarations.md`.
3. Complete the store listing from `listing.md`.
4. Under Testing, then Internal testing, upload
   `app/build/outputs/bundle/release/app-release.aab` and accept Play App Signing.
5. Install from Play on the S26 and run the smoke tests in `HANDOFF.md`.
6. Promote to production.

Build the bundle with `./gradlew bundleRelease`. Add 1 to `versionCode` for each upload.
