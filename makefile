.PHONY: default
default: build
	build/tester/tester struct.bb

.PHONY: compile
compile: build
	build/tester/tester struct2.bb > struct2.ll
	clang struct2.ll -o struct2
	./struct2


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
