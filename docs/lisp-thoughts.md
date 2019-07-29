Date: July 29th, 2019

Now, Lisp is cool. When I say Lisp, I mean Common Lisp and Scheme because I have
not used others. 

## Lisp the Expressive
Lisp is special to me because it was the first place I came into contact with
homoiconicity. It's cool because it has a small core semantics from which
anything can be built and it is close to Lambda Calculus. It has macros that are
very powerful and can be used to build any language feature that cannot be built
with the simpler facitilities of the implementation you are using. It has
continuations that are more general than stackframes and serve as a powerful
basis for concurrency and more difficult language constructs. Understanding
closures and capturing variables is insightful.

In combination, all of these features are very powerful and very expressive,
allowing the user of the language great flexibility in designing their own
language from a small base and to extend it with whatever they might have in
mind.

## The Excess of Flexibility
The problems I have with it come from this excess of flexibility. Lisp
implementations and the general culture of the language use this flexibility to
create powerful language constructs and expressive code, but the code is not
simple to follow. It is *too* expressive. Many speak of the issues of a Lisp
codebase where each one seems to have its own dialect of Lisp. Indeed, any
language with a powerful meta-programming suite can create disparate dialects
inside of it. But because the core of the language and the semantics are so
small, there stand many layers of macros between the programmer and much of the
computation they are doing and too much of the language seems like magic. I,
personally, like having a better grasp on what my machine is doing when I write
code. 

## C and Stackframes
Most machines I know of and the vast majority of CPUs, ISAs, OSs, Language
Runtimes, VMs, and so on all operate with the notion of stackframes. These are
the C-family counterpart to continuations (Closures are the counterpart to
structs, I guess, but the line is already blurry). Stackframes are how most
machines think. Because they are so prevalent, not being able to reason about
the stackframe or primitive types does not rest well with the mental model I
want to have when I'm reasoning about my program.

Tangent: 
You might argue that this is because C won some war between language families a
long time ago and Unix won some kind of operating system race, but I'm not
writing this as a political agenda or a statement of history. I am only looking
at the experience I have had programming.

C is a decent language. A lot of very robust software is written in C and there
is a large supply of code learn from. Its mental model is far from trivial and
there are a lot of ins-and-outs to learn to truly understand the whole language,
but it is not a large language. Almost all of its derivatives have added things,
not removed.

## The Big C Family
C++ is one such derivative. Every so often, it adds metaprogramming and
compile-time programming utilities to make sure their programs behave as
expected and the compiler does what is needed. The template system is one such
metaprogramming utility. The `constexpr` keyword is a compile-time programming
utility. C-macros are used as well, in cases that neither suffice.
Lambda-expressions are another compile-time programming utility. Many of the
language features that are being added to C++ are merely extensions of these
systems and of meta-/compile-time programming capabilities in the language:
concepts, reflection, typeid, ranges (arguably), metaclasses, and more. This
seems to abide by Greenspun's tenth rule, "Any sufficiently complicated C or
Fortran program contains an ad-hoc, informally-specified, bug-ridden, slow
implementation of half of Common Lisp"
([src](https://en.wikipedia.org/wiki/Greenspun%27s_tenth_rule)), except the C++
language itself has become that sufficiently complicated program.

Instead, we can build a language from C or some reasonable subset, add a small
amount of metaprogramming capability, and we would have exactly the same result.
The Lisp community and the very bright people at Utah, Cisco, and others have
established the Nanopass Framework for Racket, outlined [in this
paper](https://www.cs.indiana.edu/~dyb/pubs/commercial-nanopass.pdf). I draw
great inspiration from this paper, although I stumbled upon it after my initial
thoughts on the subject.

The Nanopass Framework inspires this project by helping me decide which
meta-programming utility I wanted to add to a simple language. I could add
macros. I could add templates. But I choose to instead add passes, as I believe
that passes are even more fundamental than all meta-programming utilities I wish
to have. If this does not end up being true, I will add others, but this one
should be enough.

## The issue with Lisp's macros
When a Lisp is a Lisp 1, that means that there is one namespace for both
functions and variables, like Scheme. When it is a Lisp 2, there are separate
namespaces for functions and variables, like Common Lisp. Common Lisp and Scheme
differ in another fundamental way: their macro implementation. Common Lisp
primarily uses `(gensym)` to create fresh symbols for use in their macros to
avoid polluting the outer namespace. Scheme uses its various `syntax-rules`
utilities to do so.
- [Lisp 1 vs Lisp 2](https://stackoverflow.com/questions/4578574/what-is-the-difference-between-lisp-1-and-lisp-2)

Common Lisp's `(gensym)` is a significantly simpler approach. Looking at the
same thing implemented with `syntax-rules` is very daunting. The most compelling
argument for macros with `syntax-rules`, called *hygienic* macros, is that they
are guaranteed to not pollute outer closures. Because Scheme is a Lisp 1, if you
modify some function name that the macro happens to use internally, you have to
rename it so that it is "colored" as using the global scheme version of the
function names. 
- see [hygiene-versus gensym](http://community.schemewiki.org/?hygiene-versus-gensym)

In Common Lisp, you have different utilities for modifying
the function and the value namespaces, with each entry in them being called a
`function cell` and a `value cell` respectively. If you wanted to call a function
that was in a value cell, you'd have to use `function` (or prepend `#'` to the
expression that contains the function) to access the function cell and `funcall`
to actually call the function.
- [`#'*expr*` == `(function *expr*)`](https://stackoverflow.com/questions/14021965/the-in-common-lisp)
- [`Common Lisp hyperspec function`](http://clhs.lisp.se/Body/s_fn.htm)
- [`lambda expresions in Common Lisp`](https://stackoverflow.com/questions/13213611/writing-lambda-expressions-in-common-lisp)

This leads to an idea: why not just call functions? C doesn't have this issue
because it defines each function as both syntax that is easy to call
`f(arguments...)` or as a function pointer of type `return_t
(*func_type_name)(t1 a1, t2 a2)`.

## The Parentheses in the Room
Lisp is kind of hard for people to parse. This is a general issue. Being more
explicit would probably not hurt people. I believe the issue lies in the
function call syntax being so similar to the rest of the language. `(a b)` calls
function `a` with argument `b`. It's a little too concise for me. In other
languages you have some syntax to break up the uniformity of the parentheses:
braces, brackets, angle brackets, commas. Lisp has none of these. But while
adding more syntax is fun, I am choosing to break up the monotony of the syntax
by merely adding more words inside of the expressions.

## `Texp`s: Trie expressions
`Texp`s are defined by a value at the beginning which is a character string and
children that are more `Texp`s. The value at the beginning can be used to
contain keywords and other things to help humans parse the code. They can also
be used to check for patterns and give helpful advice in the construction of
intermediate passes, allowing for understanding the relationships inside a
hierarchy of `Texp`s and their values.

I could argue for `Texp` having memory locality in their children, but many Lisp
implementations support contiguous memory data structures and I have read in the
past about many optimizations that are done to make cons-cell allocation
contiguous in certain cases.
