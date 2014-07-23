COMPONENTS=mastermind cnc figurehead core

.PHONY: default build prepare install package doc help
.PHONY: $(foreach comp,$(COMPONENTS),$(comp) $(comp)-doc)
.SILENT: help

default: build

build: $(COMPONENTS)

mastermind: core
	$(MAKE) -C host-side-tools/tbnl.mastermind/ build
mastermind-doc: 
	$(MAKE) -C host-side-tools/tbnl.mastermind/ doc

cnc: core
	$(MAKE) -C host-side-tools/tbnl.cnc/ build
cnc-doc: 
	$(MAKE) -C host-side-tools/tbnl.cnc/ doc

figurehead: core
	$(MAKE) -C guest-side-tools/tbnl.figurehead/ build
figurehead-doc: 
	$(MAKE) -C guest-side-tools/tbnl.figurehead/ doc

core: 
	$(MAKE) -C common/tbnl.core/ build
core-doc:
	$(MAKE) -C common/tbnl.core/ doc

prepare:
	SCRIPTS/00stage-prepare.sh

install: prepare
	SCRIPTS/01stage-install.sh

package: prepare
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

