adb pull "$@" /system/framework/framework.jar
dex2jar framework.jar
mv framework-dex2jar.jar framework.jar

#adb pull "$@" /system/framework/framework-res.apk

adb pull "$@" /system/framework/core.jar
dex2jar core.jar
mv core-dex2jar.jar core.jar
