#!/bin/bash
if [ "$#" -ne 1 ]; then
    echo "usage: ./build.sh <filepath>"
    echo '    where <filepath>.bb exists to create `basename <filepath>` executable in current working directory'
    exit 1
fi

# build.sh is expected to be within the cornerstone directory
# dirname to figure out relative path to cornerstone directory
#   `dirname $0` -> path to cornerstone
# cornerstone-cpp is expected to be the sibling of the cornerstone directory
#   `dirname $0`/../cornerstone-cpp
# the initial backbone file is expected to be within the current working directory
#   `pwd`
# basename to create the executable in the current directory
`dirname $0`/../cornerstone-cpp/build/driver/cornerstone `pwd`/$1.bb | clang -xir -o `basename $1` -

