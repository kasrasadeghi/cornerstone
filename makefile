runcmd=build/driver/cornerstone ../backbone-test/bb-type-tall-str/let-str.bb.type.tall.str
runcmd=build/driver/cornerstone ../backbone-test/bb-type-tall-str/store.bb.type.tall.str
# runcmd=build/driver/cornerstone lib2/core.bb.type.tall

.PHONY: run
run: compile
	./prog

.PHONY: default
default: build
	${runcmd}

.PHONY: gdb
gdb: build
	gdb -q --args ${runcmd}

.PHONY: test-gdb
test-gdb: test-build
	gdb -q --args build/test/${PROJECT_NAME}_test --gtest_color=yes

.PHONY: typer
typer: build
	build/typer/typer ../backbone-test/backbone/argcall.bb

.PHONY: compile
compile: build
	${runcmd} > prog.ll
	clang -Wno-override-module prog.ll -o prog


.PHONY: mem
mem: compile
	valgrind ./prog

PROJECT_NAME=cornerstone

.PHONY: build
build:
	@[[ -d build ]] || mkdir build
	@cd build; cmake -DGTEST=False ..; make -j8

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
