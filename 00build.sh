#!/bin/bash - 

echo 'Prerequisite:'
echo '* JDK (JDK 8): http://www.oracle.com/technetwork/java/javase/downloads/index.html'
echo '* Leiningen: http://leiningen.org/'
echo '* Android SDK (SDK 18): https://developer.android.com/sdk/index.html'
echo '- Android SDK can be found under "$HOME/android-sdk/" (symlink is acceptable).'
echo '- "tools", "built-tools", and "platform-tools" can be found on $PATH.'
echo '* dex2jar: https://code.google.com/p/dex2jar/'

( cd common/tbnl.core/; time lein check && ./00lein-install.sh )
( cd host-side-tools/tbnl.mastermind/; time lein uberjar )
( cd guest-side-tools/tbnl.figurehead/; [[ -d android-sdk ]] || ./00prepare-full-android-sdk.sh; time lein do clean, release )
