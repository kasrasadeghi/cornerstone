Date: July 29th, 2019

A grammar is a set of rules in a Texp in a file. A Texp has two things: a value,
and children. Children are contiguous, accessible by a field that is a pointer,
managed by a dynamically-sized array. The value of the Texp is a string,
currently using `std::string` from the C++ STL.

Grammars are represented as Texps, thus the rules inside of a grammar are just
the children of the root grammar Texp. Because each file is parsed with the
possibility of containing more than one Texp, you might have to get the first
child (`texp[0]`) of a grammar that is parsed from the file. Each rule has a
value, usually capitalized, that is used to direct the grammar matcher in
lookup for a child's patterns. Each rule also has a single child, which can be
thought of as a template for the children to match against. Thus I will use
*template* to refer to the only child of each rule.

Each rule in a grammar either defines relationships between a Texp and its
children or the sequential choice of more rules. Sequential choice is noted by a
"|" as the value of the rule's production.

Relationships between Texps are the more sophisticated case. A Texp will match
a rule in a grammar under the following conditions:

The value of the template must match the value of the Texp. If the value of the
template is not a value-class, denoted by starting with a hash symbol (`#`),
then it must exactly match the value of the template. There are also some
template values that start with the dollar sign symbol (`$`). These are of a
powerful special case that passes the given Texp to a builtin function in the
matcher, along with the template, that may have children that act like curried
arguments. The necessity of the flexibility of the builtin functions is
currently in question, and thus may be removed in time. Value-classes and
builtin functions may also be unified under some common framework, but future
direction is not clear on this. 

After the value has been matched, the children must match. The last child of the
template may be a Kleene Star child that matches many or none of the content of
that child, for example the rule `(Struct (struct Name (* Field))` is accessed
by the `Struct` name. A matching Texp must have the value `struct`, a first
child that matches the rule `Name`, and zero or more children that match the
rule `Field`. It may also be the case that there is no Kleene Star child. In
this case, the children of the given Texp must be exact match of the rules given
for the children in the template.