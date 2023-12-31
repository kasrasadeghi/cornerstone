PROJECT_NAME=cornerstone

SHELL := /bin/bash

# bb=../cornerstone-cpp/bin/cornerstone
bb=python ../cornerstone-py/main.py
# bb=python -m pdb -c continue ../cornerstone-py/main.py  # careful not to redirect with this, pdb won't work

ifeq ($(backend),cpp)
bb=../cornerstone-cpp/bin/cornerstone
else ifeq ($(backend),py)
bb=python ../cornerstone-py/main.py
endif

$(info Using backend: $(backend))
$(info Using bb compiler: $(bb))

# ==== BINARIES ====================

.PHONY: unparser
unparser: bin build
	@${bb} lib/unparser-driver.bb > build/unparser.ll
	@clang -Wno-override-module build/unparser.ll -o bin/unparser

.PHONY: main
main: bin build
	@${bb} lib/main-driver.bb > build/cornerstone.ll
	@clang -Wno-override-module build/cornerstone.ll -o bin/cornerstone

.PHONY: test
test: bin build
	${bb} lib/test-driver.bb > build/test.ll
	@clang -Wno-override-module build/test.ll -o bin/test
	bin/test

.PHONY: matcher
matcher: bin build
	@${bb} lib/matcher-driver.bb > build/matcher.ll
	@clang -Wno-override-module build/matcher.ll -o bin/matcher
	bin/matcher seq-kleene-2

.PHONY: path
path: bin build
	@${bb} lib/texp-path-driver.bb > build/path.ll
	@clang -Wno-override-module build/path.ll -o bin/path
	bin/path

.PHONY: runner
runner: bin build
	@${bb} runner/main.bb > build/runner.ll
	@clang -Wno-override-module build/runner.ll -o bin/runner
	bin/runner ../backbone-test/input/bb-modular/hello.bb --single 'ignored'

# ==== MISC ============================

.PHONY: other
other:
	(cd ../cornerstone-cpp; make)

.PHONY: bin
bin:
	@[ -d bin ] || mkdir bin

.PHONY: build
build:
	@[ -d build ] || mkdir build

.PHONY: unparse-all
unparse-all:
	for f in lib/*; do echo $$f; bin/unparser $$f > output; mv output $$f; done

# $ build.sh examples/hello_world
# $ ../cornerstone-cpp/build/driver/cornerstone examples/hello_world.bb | clang -xir -o hello_world -
# https://stackoverflow.com/questions/24701739/can-clang-accept-llvm-ir-or-bitcode-via-pipe/24728342
#
# NOTE:
# build cornerstone-cpp in sibling folder
# clang flags:
#   '-xir'
#    '-'  = read input from stdin, enables piping
#    '-x' = choose language for input, required for '-'
#             specific option is 'ir', the LLVM Intermediate Representation Language (LLVM IR)
