GOCMD=go
GOBUILD=$(GOCMD) build
GOCLEAN=$(GOCMD) clean
GOTEST=$(GOCMD) test
GOGET=$(GOCMD) get
GO_BUILD=$(realpath ./../../build)
$(shell mkdir -p ./../../build)

BINARY_NAME=$(GO_BUILD)/the-source-box

all: test build

build:
	$(GOBUILD) -o $(BINARY_NAME) -v

clean:
	$(GOCLEAN)
	rm -rf $(GO_BUILD)

test:
	$(GOTEST) -v ./...

run:
	$(GOBUILD) -o $(BINARY_NAME) -v
	$(BINARY_NAME)

# 默认目标
default: all