#!/bin/bash
if [ "$#" -ne 1 ]; then
    echo "usage: ./build.sh <filepath>"
    echo "    where <filepath>.bb exists to create `basename <filepath>` executable in current working directory"
fi

../cornerstone-cpp/build/driver/cornerstone $1.bb | clang -xir -o `basename $1` -
