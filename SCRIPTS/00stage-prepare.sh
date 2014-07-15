#!/bin/bash - 

STAGE=${STAGE:-99stage}
STAGE_GUEST=${STAGE_GUEST:-${STAGE}/guest}
STAGE_HOST=${STAGE_HOST:-${STAGE}/host}

## clean the stage
[[ -d ${STAGE} ]] && rm -rf ${STAGE}

mkdir -p ${STAGE}
mkdir -p ${STAGE_GUEST}
mkdir -p ${STAGE_HOST}

cp TEMPLATES/install-to-guest.sh ${STAGE}

## guest side
#GUEST_PATH=${OUT}/system/
#cp ${GUEST_PATH}/framework/tbnl.jar ${STAGE_GUEST}/tbnl.jar
#cp ${GUEST_PATH}/bin/tbnl ${STAGE_GUEST}/tbnl

name=figurehead
GUEST_PATH=guest-side-tools/tbnl.${name}/
cp ${GUEST_PATH}/target/tbnl.${name}.apk ${STAGE_GUEST}/${name}.apk
cp ${GUEST_PATH}/bin/${name} ${STAGE_GUEST}/${name}

## host side
name=mastermind
HOST_PATH=host-side-tools/tbnl.${name}/
cp ${HOST_PATH}/target/uberjar/tbnl.${name}-standalone.jar ${STAGE_HOST}/${name}.jar
cp ${HOST_PATH}/bin/${name} ${STAGE_HOST}/${name}

name=cnc
HOST_PATH=host-side-tools/tbnl.${name}/
cp ${HOST_PATH}/target/uberjar/tbnl.${name}-standalone.jar ${STAGE_HOST}/${name}.jar
cp ${HOST_PATH}/bin/${name} ${STAGE_HOST}/${name}
