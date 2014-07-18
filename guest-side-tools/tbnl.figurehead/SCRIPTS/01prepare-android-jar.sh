#! /bin/bash

real_sdk_path=${1:-$HOME/android-sdk/}

android_version=18

echo real sdk path: ${real_sdk_path}

platform_path=platforms/android-${android_version}
real_platform_dir=${real_sdk_path}/${platform_path}

cp ${real_platform_dir}/android.jar android.jar

cp android.jar android-partial.jar

# partial
target=partial
rm -rf ${target}
for i in android.jar; do
    echo unpacking: $i
    unzip -qo $i -d ${target}
    echo unpacking done: $i
done

# full
target=full
rm -rf ${target}
#for i in framework.jar core.jar framework-res.apk; do
for i in framework.jar core.jar; do
    echo unpacking: $i
    unzip -qo $i -d ${target}
    unzip -qo $i -d final
    echo unpacking done: $i
done

# final
target=final
rm -rf ${target}
for i in framework.jar core.jar; do
    echo unpacking: $i
    unzip -qo $i -d ${target}
    unzip -qo $i -d final
    echo unpacking done: $i
done

# final processing

## NOTE: the released Android.jar is known to work with Clojure; what we need is merely the *declaration* of spare classes; WE do NOT want the  
cp -r partial/* final

## manual over-ride
for i in $(cd full; find . -iname '*inputmanager*' -o -iname '*activitymanager*'); do
    cp -v full/${i} final/$(dirname ${i})
done

jar cf android-final.jar -C final .
