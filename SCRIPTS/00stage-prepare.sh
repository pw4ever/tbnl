#!/bin/bash - 

STAGE=${STAGE:-99stage}
STAGE_GUEST=${STAGE_GUEST:-${STAGE}/guest}
STAGE_HOST=${STAGE_HOST:-${STAGE}/host}

# clean the stage
[[ -d ${STAGE} ]] && rm -rf ${STAGE}

mkdir -p ${STAGE}
mkdir -p ${STAGE_GUEST}
mkdir -p ${STAGE_HOST}

cp TEMPLATES/install-to-guest.sh ${STAGE}

# guest side
#GUEST_PATH=${OUT}/system/
#cp ${GUEST_PATH}/framework/tbnl.jar ${STAGE_GUEST}/tbnl.jar
#cp ${GUEST_PATH}/bin/tbnl ${STAGE_GUEST}/tbnl

GUEST_PATH=guest-side-tools/tbnl.figurehead/
cp ${GUEST_PATH}/target/tbnl.figurehead.apk ${STAGE_GUEST}/figurehead.apk
cp ${GUEST_PATH}/bin/figurehead ${STAGE_GUEST}/figurehead

# host side
HOST_PATH=host-side-tools/tbnl.mastermind/
cp ${HOST_PATH}/target/uberjar/tbnl.mastermind-*-standalone.jar ${STAGE_HOST}/mastermind.jar
cp ${HOST_PATH}/bin/mastermind ${STAGE_HOST}/mastermind
