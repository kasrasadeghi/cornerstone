default: test

PROJECT_NAME=cornerstone-cpp

.PHONY: build
build:
	@[[ -d build ]] || mkdir build
	@cd build; cmake -DGTEST=False ..; make -j8

.PHONY: run
run: build
	cd build; ./${PROJECT_NAME}

.PHONY: test-build
test-build:
	@[[ -d build ]] || mkdir build
	cd build; cmake -DGTEST=True ..; make -j8

.PHONY: test
test: test-build
	cd build; ./${PROJECT_NAME}_test

.PHONY: test\:%
test\:%: test-build
	cd build; ./${PROJECT_NAME}_test --gtest_filter=$*

.PHONY: list-tests
list-tests: test-build
	cd build; ./${PROJECT_NAME}_test --gtest_list_tests

.PHONY: clean
clean:
	rm -rf build
