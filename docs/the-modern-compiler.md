Things get faster when you do a lot of the same thing at the same time.
Developers in the game-dev community solve this by an entity component system
that batches allocation of not only objects, but also each of the components
each object might have. This allows them to iterate all objects that share a
subset of components very quickly and a small extension of this facility allows
for runtime modification of the components an object has.

## make parsing easy
- homoiconicity, use parentheses
- lex by space
- separate notation and tree regex matching
- use sequential choice <- PEG/packrat parsing

## batch memory optimization
### separate memory allocation and initialization
### defer > raii or destructors considered harmful
### move semantics are really complicated
- copying/moving to already allocated memory

## proof algebra
### proof liquidity
- runtime/compiletime
### verification algebra
- string algebra
- number algebra
- env algebra
### grammar algebra
#### grammar compatibility between passes
- grammar.subset, rule.add, rule.replace, rule.remove

## phase ordering attack
### index grammar elements by useful names by reading proof
### pass environments
### upward propagation analysis / subtree limit analysis
### manual hierarchy of passes
