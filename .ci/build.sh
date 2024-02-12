#!/bin/bash
if [ ! -z "${RELEASE_KEY}" ]; then
    export ANDROID_KEYSTORE_FILE="${GITHUB_WORKSPACE}/release-key.jks"
    base64 --decode <<< "${RELEASE_KEY}" > "${ANDROID_KEYSTORE_FILE}"
fi

chmod +x gradlew
./gradlew assembleRelease || exit 1

if [ ! -z "${RELEASE_KEY}" ]; then
    rm "${ANDROID_KEYSTORE_FILE}"
fi