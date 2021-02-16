#!/bin/bash

# if argc != 2
if [ "$#" -ne 1 ]; then
    echo "usage: ./build.sh <filepath>"
    echo '    where <filepath>.bb exists to create `basename <filepath>` executable in current working directory'
    exit 1
fi

# TODO make less bad
# build.sh is expected to be within the cornerstone directory
# dirname to figure out relative path to cornerstone directory
#   `dirname $0` -> path to cornerstone
# cornerstone-cpp is expected to be the sibling of the cornerstone directory
#   `dirname $0`/../cornerstone-cpp
# the initial backbone file is expected to be within the current working directory
#   `pwd`
# basename to create the executable in the current directory
`dirname $0`/../cornerstone-cpp/bin/cornerstone `pwd`/$1.bb | clang -xir -Wno-override-module -o `basename $1` -
