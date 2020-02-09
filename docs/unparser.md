TODO define unparser

# Unparser Overview

The unparser initially seems like a rather strange project, but it has
meaningful contribution down the line. It is a precursor to work that
will be done to the Patcher and it will be a key step in incrementing
past the naive parser.

## Key Concepts

Unparser related keywords:
- linear types
- parser combinators
- full parsers

We use the concept linear types because an unparser that faithfully
reproduces the original program text (faithful is modulo whitespace)
must not lose information. A linearly-typed parameter must be used once
and only once. There are thus two relaxations: 1. use at most once
and 2. use at least once. At-most-once could be used for garbage
collection, similar to linear types but allowing dropped
data. At-least-once is more useful to us, because we want the data
that is parsed to be represented in the resultant cached data for the
parser and thus for the input program text to be reconstructable from
the input. We need no data to be lost.

Parser combinators allow us to think about parsers modularly and allow
us to model a parser of some specific data-type X as a function that is
given a stream that return back a pair: a member of X that was parsed
from the front, along with the tail of the input stream where the
parsed element ended on. This also gives meaning to what a Parser of X
is if it is a linearly-typed function: it must be the case that the
input stream is reconstructable from the parsed data and the output
stream.

A full parser is my name for a parser that can completely reconstruct
the input information from the output. There are different methods for
constructing a full parser: 1. parsing the extraneous information (WS
for "whitespace", which is most of the extraneous information) inline
in the data representation, for them to be stripped out
later; 2. parsing WS to some kind of lookup table; 3. parsing WS to a
parallel fold of the parsed procedure.

Instead of parsing whitespace specifically, we store the location of
each Texp that is parsed, and the closing brackets for the Texps
represented as Lists. We also need to store the starting location for
the values, but for now we assume they always start right after the
opening paren.

TODO ^ update when cache value location for List Texps

Parallel folds are a pretty cool idea. The idea is that you parse in
the same order that you unparse, at least to some degree, so if you
cache information cleverly while parsing you can cache it in exactly
the same order that you need it while you unparse, which makes sense
as both are done without (much ... you might consider one character
for string literals and comments) backtracking or lookahead. This
avoids the need for a lookup table that maps from texps to the
location of the texp.

# Related and prior work

other related concepts:
- trees that grow
- data-oriented programming
- entity component systems

These are other strategies for accumulating information, for example
the Trees That Grow paper from the Haskell community. The approach in
some OOP systems is to simply inherit from a base class and add more
fields. The approach that I draw inspiration from I actually did not
see in a compiler/PL context, but in a game technology class. 

Game often use _entity component systems_ to keep track of the objects
in the game. This is an implementation strategy for _data-oriented
programming_, which rests (afai-understand) in large part on the
preference for Structs of Arrays (SoA) over Arrays of Structs
(AoS). Data-oriented allows for the dynamic modification of properties
for a data type by adding or deleting an array in a SoA, which often
represents a data store for a specific family of objects that grow
together.

This is similar to how an allocator for a specific type of object
works (slab allocation). The allocator does not need to know how large
the size is for the object because it is only allocating one type of
object, and thus all are of the same size.

# Future work

This will eventually lead to work with a specific Texp allocator and a
system for managing accumulated properties.

The unparser, as mentioned above, it vital to the Patcher.

The work done in the unparser also required modifications to the
Reader and the Parser to store parsed information, which should be
useful in a learning formatter, a linter, and error reporting after
the passes.

This also prompted the separation of reified and conceptual Texps.
