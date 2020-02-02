# Backbone and Cornerstone Wisdom

Language Format
- The first thing in the group of a parentheses is a keyword or
  otherwise special
- Struct names always start with `%struct.`
  
Strings
- "You must zero terminate your own strings (currently)\00"
- There are no escaped characters, only escaped hex values
  - You can use python to figure out the hex for an escaped character
    in a string
    - `hex(ord("\""))` or `hex(ord("\n"))`
  - or find the numerical ascii code for a character `ord(' ')`
  - The ascii code for a character can be put inside a i8

Grammar
- Read [the Backbone Grammar](grammars/bb-include.lang).
- Arguments go in a function call, Parameters go in a function
  definition.
- Names and values come before types, ex:
  - `(0 i8)` for a type-qualified literal
  - `(%a u64)` for a parameter

Naming
- Global names start with `@`
  - Currently the only global names are functions
- Local names start with `%`
  - Currently the only local names are values

Core lib conventions
- Function names in the backbone core library look strange
  - the `.` in `@String.makeEmpty` has no special significance; this is the
    convention I use to namespace functions.
  - Functions of the form `@A$ptr.foo` that take in 
    `(%this %struct.A*)` as their first parameter can be used like methods.

Commutativity
- Non-commutative mathematical binary operations might seem strange at first.
  - This is the rule `(- <a> <b>)` -> `<a> - <b>`.
  - This works for `- < > >= <=` and any others.
  - I am considering flipping them. 
    - This may seem strange, but to me reading `(- 1 %a)` is easier
      than `(- %a 1)` because the minus and the `1` are close
      together.

Usage
- prefer `u64` over `i32` and `i64`
- You can declare any function in the C standard library and use them,
  even varargs functions:
  - `(decl @printf (i8* ...) i32)`
- Values cannot be mutated. To mutate use local variables.
- To allocate local variables, use `auto`. See [examples/auto.bb](examples/auto.bb)
- Let must be the result of computation
  - i.e. you cannot `(let %a (5 u64))` you must `(let %a (+ 5 (0
    u64)))`
  - you definitely cannot `(let %a 5)` because `%a` would not have a type.
- functions must always return
  - void functions use `(return-void)`
  - non-void functions use `(return <expr>)`
- `main` is the same as C. Optionally `(params (%argc i32) (%argv i8**))` returns i32.

Zen
- Say What You Mean (SWYM)
  - Having nice error messages is the Compiler doing SWYM
- Pointers > Reference semantics
- Be clear and concise
- Names come first
- Language is just an AST
- Everything is a Tree
  - Texps ~= Trees
  - Everything is a Texp
- Make everything as simple as possible
- Recursion > Loops
  - Loops are an optimization
- Think in Code, not in templates/macros/lazy functions/etc
  - People already know if-statements and function calls
  - Most abstractions are "these two pieces of code look similar"
- Try to make everything legible
  - Even "library code" should be understandable
- Passes > Macros
- Code, **then** refactor
  - Do not pre-optimize
- `kabob-case` is just as legible as `snake_case` but easier to type
- Try not to give people RSI
  - Don't create things that require double symbols, like `std::cout`
