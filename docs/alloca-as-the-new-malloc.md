http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.112.5538&rep=rep1&type=pdf

This paper describes "lazy allocation" where the only allocation available to
the programmer is stack allocation and when a reference to a stack-allocated
object escapes the current stackframe, it is moved to the heap. Therefore you
never need malloc. So when do you free? When the pointers go out of scope, I
guess.

This is a neat idea. It's a simpler mental burden than r-value references and
move-semantics. It's kinda java-esque. In fact I'm pretty sure this is the
converse of Escape Analysis which is used in the HotSpot VM for Java.