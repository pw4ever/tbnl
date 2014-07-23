# author: Wei Peng <write.to.peng.wei@gmail.com>

BUILD_TIMESTAMP_FNAME:=_timestamp_

COMPONENTS:=core mastermind cnc figurehead

PREFIX_core:=common/tbnl.core
DEP_core:=

PREFIX_mastermind:=host-side-tools/tbnl.mastermind
DEP_mastermind:=core

PREFIX_cnc:=host-side-tools/tbnl.cnc
DEP_cnc:=core

PREFIX_figurehead:=guest-side-tools/tbnl.figurehead
DEP_figurehead:=core

.PHONY: default build prepare install package doc help all all-install
.PHONY: force
.PHONY: $(foreach comp,$(COMPONENTS),prebuild-$(comp) doc-$(comp) clean-$(comp) distclean-$(comp))
.SILENT: help

default: all
all: build doc prepare package
all-install: all install

get_target=$(PREFIX_$(1))/$(BUILD_TIMESTAMP_FNAME)
get_dep=$(if $(DEP_$(1)),$(PREFIX_$(DEP_$(1)))/$(BUILD_TIMESTAMP_FNAME),)


prebuild: $(foreach comp,$(COMPONENTS),prebuild-$(comp))
build: prebuild $(foreach comp,$(COMPONENTS),$(call get_target,$(comp)))
doc: build $(foreach comp,$(COMPONENTS),doc-$(comp))
clean: $(foreach comp,$(COMPONENTS),clean-$(comp))
distclean: $(foreach comp,$(COMPONENTS),distclean-$(comp))

define comp_body
$$(call get_target,$(comp)): $$(call get_dep,$(comp)) | prebuild
	$$(MAKE) -BC $$(PREFIX_$(comp)) build
prebuild-$(comp): $(if $(DEP_$(comp)),prebuild-$(DEP_$(comp)),)
	$$(MAKE) -C $$(PREFIX_$(comp)) build
doc-$(comp): | build
	$$(MAKE) -C $$(PREFIX_$(comp)) doc
clean-$(comp):
	$$(MAKE) -C $$(PREFIX_$(comp)) clean
distclean-$(comp):
	$$(MAKE) -C $$(PREFIX_$(comp)) distclean
endef

$(foreach comp,$(COMPONENTS),$(eval $(comp_body)))

prepare: | build
	SCRIPTS/00stage-prepare.sh

install: | build prepare
	SCRIPTS/01stage-install.sh

package: | build prepare
	tar cvf tbnl.tar tbnl

help:
	echo 'Prerequisite:'
	echo '* JDK (JDK 8): http://www.oracle.com/technetwork/java/javase/downloads/index.html'
	echo '* Leiningen: http://leiningen.org/'
	echo '* Android SDK (SDK 18): https://developer.android.com/sdk/index.html'
	echo '- Android SDK can be found under "$$HOME/android-sdk/" (symlink is acceptable).'
	echo '- "tools", "built-tools", and "platform-tools" can be found on $$PATH.'
	echo '* dex2jar: https://code.google.com/p/dex2jar/'
	echo
	echo '(default - all)'
	echo 'build       - build components'
	echo 'doc         - update doc'
	echo 'prepare     - prepare for install & package'
	echo 'install     - install to device'
	echo 'package     - package into tbnl.tar'
	echo 'clean       - clean components'
	echo 'distclean   - clean components completely'
	echo 'all         - build + doc + prepare + package'
	echo 'all-install - all + install'
	echo 'help        - you are reading it'

