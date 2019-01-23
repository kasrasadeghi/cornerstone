PROJECT_NAME=cornerstone-cpp

.PHONY: build
build:
	@[[ -d build ]] || mkdir build
	@cd build; cmake ..; make

.PHONY: run
run: build
	cd build; ./${PROJECT_NAME}

.PHONY: test-all
test: test\:nopdecl.bb test\:hello-world.bb

.PHONY: test\:%
test\:%: build
	if [[ -f $* ]]; then build/${PROJECT_NAME} < $*; \
	else build/${PROJECT_NAME} < ../backbone-test/base/$*; fi

# build/${PROJECT_NAME} < nopdecl.bb
.PHONY: test0
test0: build
	build/${PROJECT_NAME} < ../backbone-test/base/hello-world.bb

.PHONY: test2
test2: build
	build/${PROJECT_NAME} < ../backbone-test/base/struct.bb

.PHONY: clean
clean:
	rm -rf build
