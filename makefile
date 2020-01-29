PROJECT_NAME=cornerstone

.PHONY: main
main:
	../cornerstone-cpp/build/driver/cornerstone lib/main-driver.bb > prog.ll
	clang -Wno-override-module prog.ll -o prog
	./prog

.PHONY: matcher
matcher:
	../cornerstone-cpp/build/driver/cornerstone lib/matcher-driver.bb > prog.ll
	clang -Wno-override-module prog.ll -o prog
	./prog exact

.PHONY: gdb
gdb:
	gdb -q ./prog

.PHONY: mem
mem: compile
	valgrind ./prog

.PHONY: other
other:
	(cd ../cornerstone-cpp; make)

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
