.PHONY: default
default: build

.PHONY: typer
typer: build
	build/typer/typer ../backbone-test/backbone/argcall.bb

.PHONY: compile
compile: build
	build/tester/tester struct.bb > struct.ll
	clang struct.ll -o struct
	./struct

PROJECT_NAME=cornerstone

.PHONY: build
build:
	@[[ -d build ]] || mkdir build
	@cd build; cmake -DGTEST=False ..; make -j8

.PHONY: run
run: build
	cd build; ./main/${PROJECT_NAME}

.PHONY: test-build
test-build:
	@[[ -d build ]] || mkdir build
	cd build; cmake -DGTEST=True ..; make -j8

.PHONY: test
test: test-build
	build/test/${PROJECT_NAME}_test --gtest_color=yes

.PHONY: test\:%
test\:%: test-build
	build/test/${PROJECT_NAME}_test --gtest_filter='*$**'

.PHONY: run\:%
run\:%: build
	build/driver/${PROJECT_NAME} $*

.PHONY: list-tests
list-tests: test-build
	build/test/${PROJECT_NAME}_test --gtest_list_tests

.PHONY: clean
clean:
	rm -rf build
