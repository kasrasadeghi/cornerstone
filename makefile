.PHONY: run
run:
	[[ -d build ]] || mkdir build
	cd build; cmake ..; make
	cd build; ./cornerstone