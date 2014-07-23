BUILD_TIMESTAMP=_timestamp_
SRC:=$(shell find src -type f) project.clj

.PHONY: default build doc clean

default: build

build: $(BUILD_TIMESTAMP)

$(BUILD_TIMESTAMP): $(SRC)
	lein uberjar && touch $(BUILD_TIMESTAMP)

doc: docs/uberdoc.html
docs/uberdoc.html: $(BUILD_TIMESTAMP)
	lein marg

clean:
	lein clean
