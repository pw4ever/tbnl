#! /bin/bash
keytool -genkeypair -keyalg RSA -keystore $HOME/.android/debug.keystore -storepass android -alias androiddebugkey -keypass android -dname 'CN=Android Debug,O=Android,C=US'
