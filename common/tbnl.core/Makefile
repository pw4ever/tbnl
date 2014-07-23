# author: Wei Peng <write.to.peng.wei@gmail.com>

BUILD_TIMESTAMP:=_timestamp_
SRC:=$(shell find src -type f) project.clj

.PHONY: default build doc clean dist-clean

default: build

build: $(BUILD_TIMESTAMP)

$(BUILD_TIMESTAMP): $(SRC)
	lein check && ./00lein-install.sh && touch $(BUILD_TIMESTAMP)

doc: docs/uberdoc.html
docs/uberdoc.html: $(BUILD_TIMESTAMP)
	lein marg

clean:
	lein clean

dist-clean: clean
	rm -rf $(BUILD_TIMESTAMP)
