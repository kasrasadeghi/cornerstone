# Cornerstone

Cornerstone is a compiler framework. You can use it to build compilers
easily. I'm using it to build the Backbone compiler easily.

## TL;DR

```bash
curl https://github.com/kasrasadeghi/cornerstone/blob/master/installer.sh | bash
cornerstone/build.sh cornerstone/examples/hello_world
./hello_world
```

## Install

Make sure you read [installer.sh](installer.sh) first.

```bash
curl https://github.com/kasrasadeghi/cornerstone/blob/master/installer.sh | bash
```

To not directory use `master`'s installer, use this:

``` bash
curl https://github.com/kasrasadeghi/cornerstone/blob/8745b545b2b30ce6683a6b1f077ecf9de4f10052/installer.sh | bash
```

They both clone `cornerstone` and `cornerstone-cpp` directly from master.

## Dependencies

might need to install:
- CMake
- Clang, LLVM

probably already have:
- GNU Make
- Bash
- A computer
- Curl/wget

## Backbone

Backbone is the backbone of the Cornerstone compiler
framework. It's the language the compiler framework is written
it. It's compiler is written using the framework. It's compiler is all
you need to write to bootstrap yourself into the Cornerstone
ecosystem. The cpp implementation of both a part of the Cornerstone
compiler framework and the full Backbone compiler is provided in
`cornerstone-cpp` and utilized in `installer.sh`.

Some examples of backbone are provided in [examples/](examples/).

# Further Reading

- [Cornerstone Crash Course](docs/cornerstone-crash-course.md)
- [Backbone Wisdom](docs/backbone-wisdom.md)
- [Making a Grammar](docs/tutorial-making-a-grammar.md),
  also goes over how the matcher works.
- [Why not a Lisp?](docs/lisp-thoughts.md)

# Repository Listing

This is the central repo for the Cornerstone compiler framework.
It contains the implementation of Cornerstone in the Backbone
programming language.
Backbone is implemented using a bootstrapping implementation. The
current reference bootstrapping implementation is `cornerstone-cpp`.

Other repos are listed below.
- [backbone](https://github.com/kasrasadeghi/backbone), the original
  backbone compiler, written in C. This is the first project in the
  chronology of the cornerstone project.
- [cornerstone-cpp](https://github.com/kasrasadeghi/cornerstone-cpp),
  the bootstrapping implementation of the Backbone programming
  language. Used to compile the Cornerstone project in this repository
  and currently used to run examples for the Backbone language.
- [backbone-tests](https://github.com/kasrasadeghi/backbone-tests),
  contains code for testing both the Backbone compiler implementation
- [ktodo](https://github.com/kasrasadeghi/ktodo), is a webkit-based
  todo viewer and manager.

Other partial implementations for exploratory work are listed
below. These may be interesting for people looking for implementations
in different languages or styles. Most implementations contain at
least the parser, some have varying amounts of the compiler passes and
compliance with the cpp implementation.
- [cornerstone-d](https://github.com/kasrasadeghi/cornerstone-d)
- [cornerstone-haskell](https://github.com/kasrasadeghi/cornerstone-haskell)
- [cornerstone-kotlin](https://github.com/kasrasadeghi/cornerstone-kotlin)
