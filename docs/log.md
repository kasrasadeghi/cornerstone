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