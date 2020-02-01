# 2019 jan 16, 7 am

Grammar Construction
  parse grammar into lines
  split line on first whitespace blob
    left is type
    right is production
  parse productions into Texps
  prepare bidirectional type <-> string_view lookup
    read through the grammar
      check for new types
        each type is denoted as starting with a capital letter
          ? instead of capital letter, maybe just the first words in each line
      put type in ordered set of type names, underlying vector
        the index of that type name will be the "Type" value that is returned and ->
          will be uniquely comparable
        if the type name is already present in the vector, do nothing
