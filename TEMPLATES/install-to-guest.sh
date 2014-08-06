#!/bin/bash - 

ADB=${ADB:-adb}
STAGE=${STAGE:-.}
STAGE_GUEST=${STAGE_GUEST:-${STAGE}/guest}
STAGE_HOST=${STAGE_HOST:-${STAGE}/host}

adb="${ADB} $@"

## guest side

${adb} root
${adb} remount

${adb} push ${STAGE_GUEST}/figurehead.apk /system/app/
${adb} push ${STAGE_GUEST}/figurehead /system/bin/
${adb} shell chmod 700 /system/bin/figurehead
#${adb} install -r ${STAGE_GUEST}/figurehead.apk

# test
${adb} shell figurehead -h

## host side
