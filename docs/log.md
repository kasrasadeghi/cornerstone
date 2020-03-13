# mar 13
# "ownable"
only something that is ownable can allocate memory in its ctor
- maybe only allow allocation in ownable object ctors

# mar 11
# subset grammar
you can have a grammar be a subset of another grammar through many
means.

a simple mean is where one production just turns into other
productions in a lower grammar:

```
(Become (become @function (args ...)))

-> if return-type != void: (return (call-tail @function (args ...)))
-> otherwise: (do (call-tail @function (args ...)) (return-void))
```

# mmap
cannot mmap empty files. mmap crashes with "illegal argument"
TODO fix that, maybe give a better error message

# feb 18
# grammar normalization
- say you have a grammar with

(Ints (#name (* #int)))

but oh no, #int is not a production name

so just extract it to a production name, like so

symbol = gensym()
,symbol is unquote

Grammar:
(Ints    (#name (* ,symbol)))
(,symbol (#int))

# feb 17
# normal form optimizations
- if a texp is in single-line normal-form then there is no need to do
  complicated unparsing, just run Texp.parenPrint()

# feb 14
# factor expressions
we should have a certain kind of expression that only has atomic
children that get evaluated in a factor like language

(factor 1 2 3 + +) <- like that

# feb 13
# back-reference while parsing
you should be able to look at the tree you're currently constructing
while you're parsing it in order to make decisions, especially error
reporting

- does this kind of parsing make parsing context-sensitive?
>>>>>>> log: grammar normalization

# feb 12
# splitting allocations
`let udev = user-developer, a developer that is using a library or tool`

there's no way to split an allocation into two spans.

**freeing spans is hard**
this is problematic for free:
- let a span of memory `[----C----]` is split into `[--A--][--B--]`
- the owner of C is responsible for freeing C, which turns into A.
- but what if A frees before B? should B not also own its memory?

**header and footer**
this is a pretty hard problem because usually memory allocators
require header and footer information. this would only be possible
with slab allocators or range based allocators, or allocators that put
header and footer information outside of the allocated range.

- **free(ptr, size)**
this may be solved by just passing the size that we want to free so
that it knows how much of the range afterwards is free'd.

# allocation size is usually last member
often the last member of a struct contains the allocated size.
- can set the last member of a struct and then call @resize to
  actually make it that size
- can use the last member of a struct instead of taking extra space,
  as long as the udev is a faithful actor
  - if the udev is not faithful, then mmap him and make it a user
    library so he can't screw up the system allocator

# feb 10
# subtyping through cast to first member like C
might be useful for static strings
- add different types for static strings, zero terminated strings,
  string views, i8*s, and strings with different allocation strategies
  - view-based allocation strategy
    - if two strings are the same length and have the same content, we
      can use a central allocator that doesn't allocate extra space
      but instead points to the previous one from a large array
  - content-based substring allocation strategy
    - if there exists that string's content in the allocated string
      memory, point to it and (by necessity returning a view) return a
      length
    - ex: allocating "hello" after "hello world" would not consume any
      more memory, instead returning the same pointer as "hello world"
      but with a length of 5

# multimethods and methods implementation
- methods look up themselves on a table based on their first argument
  - currently expands to T$ptr.method-name
- multimethods look up themselves up based

# lazy merge sort
declaring something as the merge of two lists with a comparison
operator just takes the lesser of the two at each iteration. this can
be used to merge streams.

this idea is used in the current unparser.

# feb 8
# many or command
Often times in an infix operator language i wish to say "A or B is
equal to C" or "A and B is equal to C" which could be "A or B == C" "A
and B == C" except that that overloads "or" and "and" to mean both
binary or/and but also grouping.

One possible remedy would be to have "A, B == C" for grouping 'and'
and "A / B == C" for grouping 'or'. This would overload the divide
operator, but we could use 'int./' or '//' to disambiguate.

In a Texp/prefix syntax, we could do:
`"A or B is equal to C" => "(== C (some A B))"`
`"A and B is equal to C" => "(== C (all A B))"`
which lends itself to the 'for all' and 'there exists/some' symbols in
mathematics.

We could use 'exists' instead of 'some', 'both' instead of 'all',
'atleast-one-of' instead of 'exists'. 'atleast-one-of' is certainly
more clear, but very verbose. 'some' is balanced between
real-language-sounding (unlike 'exists'), and concise (unlike
'atleast-one-of').

# kinds of errors
there's a difference between assertions for correctness, user
behaviour, and system behaviour, and developer user errors
- correctness assertions should crash and some of them should be
  removed in a 'release' build
- user behaviour errors should be reported so that they can know
  something went wrong and how to fix it
- developer user errors should crash and show debug information, like
  stacktraces
- system behaviour errors should crash and say something that would be
  helpful to look up, like "program exceeding memory consumption
  expectations" or something.
  
Some user errors can even be ignored or worked around to continue a
brief amount to give the user a better understanding of the error
message.

A correctness assertion might also be called an internal consistency
check.

# feb 6
there's a difference between uninitialization and freeing.
Uninitialization frees the contents of a structure.
Freeing deallocates the storage of the structure.

These two ideas should be separated, maybe to be combined in "destroy".

# jan 26
'#name's let you pick paths

# jan 16
# struct ergonomics
We can possibly add 'ref' and 'get' that take field names to improve
struct ergonomics. 'get' would be equivalent to a load-index and 'ref'
would be equivalent to just an index.

# jan 15, 2020
# matcher done?
Now that the matcher is basically done, I have to figure out what to
do next.

1. investigate "base element of getelementptr must be sized" for
   accessing first element of a struct
2. not leak memory
3. detach from cmake and reconfigure build system
4. swap index and store
5. add extract and insert/update, add auto with initialization and
   type inference
6. add type checker notation
7. debug information

# jan 14, 2020
# checked, proof, and unsafe
Functions that have to check something in order to function have a lot
of options.

**oop #1**: Check it and return null otherwise.

**oop #2**: Throw an exception.

**optional**: Check it and return an optional none if the predicate fails.

**unsafe**: Just do it and crash if it fails.

**checked**: Do it and check in order to crash gracefully.
- like #1 but you don't want people to catch this exception, like an assertion

**proof**: In languages with more advanced compile-time checking,
such as dependent types or contracts, you can also require that the
condition be checked by something else beforehand or simply correct by
construction.

Many languages also make the difference between developer-of-module
errors and user-of-module errors, where the developer-of-module errors
should never occur and the user-of-module errors have nice
information and are about some kind of incorrect input to the program
instead of bad program behaviour. Unix does this with exit codes.

I like the combination of checked, proof, and unsafe, but I have not
yet integrated those three ideas with the developer vs user error
difference. I have also not yet figured out how to make a template so
that all three of {checked, proof, unsafe} can be generated from a
single definition or code sample, which is similar to how a language
with powerful contracts would work.

# aug 19, 2019
# reduction to catamorphic recursion
Because I'm implementing passes instead of macros as the foundation for
metaprogramming in the language, there are some interesting consequences. Passes
can be implemented with arbitrary code, but using the system I am envisioning
that is similar to the Nanopass system, passes are restricted to catamorphic
recursion instead of general recursion. This will make passes easier to reason
about as they only rely on a base case and inductive steps. Of course, there
will be tooling for in-order Texp recursion and environment accumulation, so you
can escape restricted catamorphism using that set of tooling, but it will still
simplify the structure of the recursion and the base case for a lot of work and
help programmers focus on what things actually change and how parts of the
language affect other parts.

# alternate word parsing rule
Maybe we can have words parsing until not only just a special character, but
only until whitespace. Or only until whitespace and close-paren that is
unmatched within the atom. This would allow for '.+(1)' and '.cast(64)' to be
parsed within a single atom and it would strengthen subatomic parsing. Of
course, we have other grouping symbols that are nonspecial so this is kind of
redundant, but having the parser be more flexible is not bad.

# subatomic parsing
The cornerstone parser lexes by space with a single pair of special
nonwhitespace characters: the parentheses ( '(' and ')' ). In order to implement
certain kinds of language syntax, you often have to parse other special
characters which would be grouped into a single atom by the cornerstone parser.
Consider `java obj` below. This allows you to implement more flexible kinds of
parsing inside of each atom, bypassing the simple builtin atomic parsing.
Because this happens below atomic parsing, I'm calling it "subatomic parsing".

# special support for indent nesting
Because many languages require support for whitespace tangibility in their
parsers, it will be difficult to support many styles of syntax natively.

```
define whitespace tangibility: parsing and reading how much whitespace and what
  kind is between two tokens; it not being sufficient to know that there is
  merely whitespace between two tokens.
```

Python, Haskell, and the ML family are some examples of languages that require
whitespace tangibility for their parsers. What is common in these is that they
require whitespace tangibility only to use intentation as a method of nesting.
This is similar to the .tabs() printer for Texps. This further motivates future
word on special support for indentation sensitive parsing.

The indentation specific parser and the pretty printer are duals of each other.

# parenless nesting
```lisp
; original, backbone.type.tall
(let %new-char-loc (cast i8* (+ 1 (cast u64 (load (%ptr-ref))))))

; factor mode
(let %new-char-loc (|> %ptr-ref load u64 cast 1 + i8* cast))

; ocaml pipes
(let %new-char-loc (pipes %ptr-ref |> load |> cast u64 |> + 1 |> cast i8*))

(let %new-char-loc (pipe %ptr-ref load (cast u64) (+ 1) (cast i8*)))

(let %new-char-loc (pipe %ptr-ref load . cast u64 . + 1 . cast i8*))

; java obj
(let %new-char-loc (mthdcall %ptr-ref .load .cast[u64] .+[1] .cast[i8*] ))

; haskell precedence
(let %new-char-loc (autoprec cast i8* + 1 cast u64 load %ptr-ref))
```

These are some ideas for reducing parenthesis usage. Honestly, the first one
seems the most legible to me. `java obj` and `ocaml pipes` are fairly legible
too, and I like that I can read them in order instead of math backwards, but
math backwards is pretty unambiguous and common. `factor mode` is classically
illegible to me without momentarily rewiring my brain and while that is fun to
do sometimes it does not seem fit for productivity to me. `haskell precedence`
is equivalently illegible. `ocaml pipes` and `java obj` are similarly legible,
but `ocaml pipes` stays a little bit more true to the original form so it may
help cross-legibility.

It may be worth investigating what a `smalltalk` "reader macro" would look like.

It's also worth noting that because I do not plan for my "reader macros" to lex
by anything but spaces, they'll have to do subatomic parsing themselves.

# jul 29
# name, not value
I should consider making some things in the pass after normalization Name, not
Value, as they have to be locations and neither uncasted literals nor str-gets
can be locations. Functions (like `malloc or calloc`) can return locations and
you can just get locations from `auto`. You can cast int-literals to locations
for kernel hacking, I guess.

In type_expand, I can make `Name()` only look up the type from the environment,
but `Value()` do the full `env.typeOf` function call.

# jul 21
# graph for texps, proofs, and grammars
Sometimes during a pass, someone may want to make a new kind of Texp that does
not have a grammar written for it. We should be able to quickly make a new
grammar production for a single Texp that "inherits" or "includes" productions
from other grammars. 

The motivational example for this behaviour is a wrapper of statements during
normalization. One statement may turn into many statements as intermediate
expressions are expanded during normalization. One `Let` becomes many. To return
many `Let`s from the processing of a single one, we should return a Texp. But if
the Texp is modified, we need not only return a Texp, but a Texp with the
grammar it has been proven to follow and the production it has been proven to
abide by. We could call a new production `Wrap` that takes many statements and
wraps them up and is expected to be "unwrapped" in its parent to be placed
inside of the new block that the original statement came from.

```
// grammar
(do
  (let b ...)
  (let a ...)
  (let c ...)
  ...
)

/* 

normalize (let a ...)
=> 
(wrap
  (let a$0 ...)
  (let a$1 ...)
  (let a ...)
)

*/
(do
  (let b ...)

  /* inserted v */
  (let a$0 ...)
  (let a$1 ...)
  (let a ...)
  /* inserted ^ */

  (let c ...)
  )
```

The proof-type for the intermediary result would require a production
```
(Wrap (wrap (* Stmt)))
```
where it plucks Stmt from whatever version of the backbone grammar it's
operating on.

# Types for functions with the productions they create and take in.

# may 29
# terminal proofs for generation
In generation, if your proof has no more choices in it, there is no more
reason to pass it along. This is because the generator is not going to use it
to switch between different generation choices. This may be able to be further
generalized for passes.


# dec 13, 5am
# Slab allocator for nodes and node components
 - would be able to copy nodes
 - pulls from component sets
 - extend capability through compiler

# 'is' the type map
 - con: how is the compiler supposed to be able to compile-time optimize if it's a static map? gotta see if it'll do it
 - it's going to be generated/made anyways.

# changing grammar
 - the grammar is going to need to change as time goes on anyways.

# something to figure out if a texp matches any part of a grammar


# dec 16, 6pm
# runtime grammar  
 - hack together runtime rules
 - generate
   - generate code and dynamically link against it
   - generate embedded language and run against that
   - generate backbone and run interpreter


# dec 17, 7pm
# should children be regex matched?  
it's pretty easy to define behaviour for a regex match for the value of a node
in a production. you just switch from the value string-eq'ing another string, to
a value regex-matching some kind of regex.

```python
def match(texp, rule):
  if rule.value == "|":
    return choice(texp, parseTypesFromChildren())
  

  if rule.value[0] == '#':
    check regexes[rule.value[1:]](texp.value)
  else:
    check rule.value == texp.value
  
  exact(texp, parseTypesFromChildren())
```

so how would we implement being able to match not just the value of a 
rule/production? the types the children match against would need to be not just
production types. they'd need to also be leaves.

Is it true that the leaves are entirely composed of regex matches,
or at least a combination of regex matches and str-eq matches? if so, then we
would have to add the regex matches to the Types Enum, or at least create a case
where a Type can either be a production or a regex match, which I think is the
regular case with grammars anyways.

This would be far easier to do if there were no Type enum, which will be the
case after the system transitions towards a fully runtime system anyways.

We'll have to eventually turn to a fully runtime grammar because the grammar
will evolve and change through different passes, so we'll have to implement it
later. So we're going to implement this child regex idea, if we even need it,
after the runtime grammar and whatever else that entails.

# dec 18, 2 am
# function call syntax for arbitrary position, not full texp
We have regexs for values and function calls for whole productions, but we could
generalize both and have just function calls for any kind of position in a
scheme, but it would be a little complicated. I think this would be an
appropriate approach after the runtime grammar transformation.

Currently we have function call syntax for a production like ($functionName),
which means "functionName" is a function that takes in a Texp and returns a
bool and the matching of this production for a Texp is defined by the function
returning true for this Texp.

We would need some kind of function call syntax for calling the function given
either a value or a whole texp, but I'm not sure what the protocol for that
would be.

# dec 19 6pm
# unique hasing on a string set
We might need to consider some way of hashing all of the strings in the program
fairly uniquely or hashing at least the keywords uniquely.

We might also want to make all of the values in the texp string_views of a
table/array or something. Probably a table of some kind because hashing seems
faster.

# dec 20 5am
# values are leaves
Values are in fact leaves, and there was a previously erroneous collision of two
separate ideas in my head. I have to make a new concept, #name, which represents
the namespace collision checking mechanism and Name, which is now separate from
value checking, but is defined as a Texp with a #name value and no children.

This collision originally came about because somewhere in my head I defined
value-checking with a non-terminal production like Name to be equivalent to
using Name's value-checker to value check a different node. So 

Name -> (#name)
Field -> (Name Type)

was defined as a Field's value must conform to #name because it is Name's
value-checker. This is mildly interesting, but it's probably not worth
investigating further because we can just use a value checker directly.
