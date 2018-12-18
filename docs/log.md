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
