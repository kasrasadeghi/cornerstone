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