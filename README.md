# HomeoRemedica for Android

Android client for [HomeoRemedica](https://github.com/rasagyavatsal/homeoremedica-app). Historical
reference only; not medical advice.

## Build

Requires JDK 17+ and the Android SDK.

```sh
./gradlew testDebugUnitTest assembleDebug
```

Builds use the production server. For a local one, run `npm start` in homeoremedica-app and
`make local`. Accounts need `app/google-services.json` from the Firebase project.

## Develop

`make` lists the commands. `make e2e` needs homeoremedica-app checked out beside this repository.

## Release

Merging release-please's PR on `main` tags a `v*` release, and the `Deploy` workflow publishes it to
Play's internal track.

## License

[MIT](LICENSE).
