#! /bin/bash

echo 'This procedure only needs to be done once.'
echo '=========================================='
echo 'Prerequisite:'
echo '* A sole device/emulator instance can be found by "adb";'
echo '* Android SDK is under '$HOME/android-sdk/' (symlink is accepted).'

SCRIPTS/00get-full-framework-jars-from-device.sh
SCRIPTS/01prepare-android-jar.sh
SCRIPTS/02setup-android-sdk.sh
