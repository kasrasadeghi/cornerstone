function is_arch_linux() {
    # check if arch is in the output of `uname -a`
    uname -a | grep arch &> /dev/null || uname -a | grep ARCH &> /dev/null;
    return $?
}

function find_command() {
    command -v $1 &> /dev/null;
    return $?
}

# arch linux package check
if is_arch_linux; then
    sudo pacman -S --needed git cmake clang
else
    echo "please ensure that `git`, `cmake` and `clang`"
    echo "are all installed and available with those commands"
fi

git clone https://github.com/kasrasadeghi/cornerstone-cpp
git clone https://github.com/kasrasadeghi/cornerstone

# set config for grammar dir to absolute path
echo "#pragma once" > cornerstone-cpp/lib/config.hpp
printf "constexpr std::string_view GRAMMAR_DIR = " >> cornerstone-cpp/lib/config.hpp
printf "\"`(cd cornerstone-cpp/docs; pwd)`\";" >> cornerstone-cpp/lib/config.hpp

# making bootstrapping compiler
(cd cornerstone-cpp; make build)

# making hello world
(cd cornerstone; ./build.sh examples/hello_world)
