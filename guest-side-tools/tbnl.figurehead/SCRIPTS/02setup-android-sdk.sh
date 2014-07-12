#! /bin/bash

real_sdk_path=${1:-$HOME/android-sdk/}
target=${2:-full}

android_version=18
fake_sdk_path=android-sdk

echo real sdk path: ${real_sdk_path}
echo fake sdk path: ${fake_sdk_path}

mkdir -p ${fake_sdk_path}

ln -sf ${real_sdk_path}/platform-tools ${fake_sdk_path}/
ln -sf ${real_sdk_path}/build-tools ${fake_sdk_path}/
ln -sf ${real_sdk_path}/tools ${fake_sdk_path}/
ln -sf ${real_sdk_path}/add-ons ${fake_sdk_path}/
ln -sf ${real_sdk_path}/extras ${fake_sdk_path}/

platform_path=platforms/android-${android_version}
real_platform_dir=${real_sdk_path}/${platform_path}
fake_platform_dir=${fake_sdk_path}/${platform_path}

mkdir -p ${fake_platform_dir}

cp android-${target}.jar ${fake_platform_dir}/android.jar
