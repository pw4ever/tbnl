# author: Wei Peng <write.to.peng.wei@gmail.com>

BUILD_TIMESTAMP:=_timestamp_
SRC:=$(shell find src -type f) project.clj

.PHONY: default build doc prepare clean dist-clean

default: build

build: $(BUILD_TIMESTAMP)

$(BUILD_TIMESTAMP): $(SRC)
	[[ -d android-sdk ]] || $(MAKE) prepare
	lein release && touch $(BUILD_TIMESTAMP)

doc: docs/uberdoc.html
docs/uberdoc.html: $(BUILD_TIMESTAMP)
	lein do droid jar, marg

prepare:
	./00prepare-full-android-sdk.sh

clean:
	lein clean

dist-clean: clean
	rm -rf $(BUILD_TIMESTAMP)
