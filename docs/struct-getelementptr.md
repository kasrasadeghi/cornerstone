# 2019 july 2, 9 am
"The type of each index argument depends on the type it is indexing into. When indexing into a (optionally packed) structure, only i32 integer constants are allowed (when using a vector of indices they must all be the same i32 integer constant). When indexing into an array, pointer or vector, integers of any width are allowed, and they are not required to be constant. These integers are treated as signed values where relevant."
- http://llvm.org/docs/LangRef.html#getelementptr-instruction
  - Chirag Patel, https://stackoverflow.com/questions/41062665/llvm-ir-getelementptr-invalid-indices
