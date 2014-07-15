COMPONENTS=mastermind cnc figurehead core

.PHONY: default build prepare install package doc help
.PHONY: $(foreach comp,$(COMPONENTS),$(comp) $(comp)-doc)
.SILENT: help

default: build

build: $(COMPONENTS)

mastermind: core
	cd host-side-tools/tbnl.mastermind/; time lein uberjar
mastermind-doc:
	cd host-side-tools/tbnl.mastermind/; time lein marg

cnc: core
	cd host-side-tools/tbnl.cnc/; time lein uberjar
cnc-doc:
	cd host-side-tools/tbnl.cnc/; time lein marg

figurehead: core
	cd guest-side-tools/tbnl.figurehead/; [[ -d android-sdk ]] || ./00prepare-full-android-sdk.sh; time lein do clean, release
figurehead-doc:
	cd guest-side-tools/tbnl.figurehead/; time lein marg

core: 
	cd common/tbnl.core/; time lein check && ./00lein-install.sh
core-doc:
	cd common/tbnl.core/; time lein marg

prepare:
	SCRIPTS/00stage-prepare.sh

install:
	SCRIPTS/01stage-install.sh

package:
	tar cvf tbnl.tar tbnl

doc: $(foreach comp,$(COMPONENTS),$(comp)-doc)

help:
	echo 'Prerequisite:'
	echo '* JDK (JDK 8): http://www.oracle.com/technetwork/java/javase/downloads/index.html'
	echo '* Leiningen: http://leiningen.org/'
	echo '* Android SDK (SDK 18): https://developer.android.com/sdk/index.html'
	echo '- Android SDK can be found under "$$HOME/android-sdk/" (symlink is acceptable).'
	echo '- "tools", "built-tools", and "platform-tools" can be found on $$PATH.'
	echo '* dex2jar: https://code.google.com/p/dex2jar/'

