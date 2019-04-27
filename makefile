default: test

PROJECT_NAME=cornerstone-cpp

.PHONY: build
build:
	@[[ -d build ]] || mkdir build
	@cd build; cmake -DGTEST=False ..; make -j8

.PHONY: run
run: build
	cd build; ./${PROJECT_NAME}

.PHONY:
test:
	cd build; cmake -DGTEST=True ..; make -j8
	cd build; ./${PROJECT_NAME}_test

.PHONY: clean
clean:
	rm -rf build
