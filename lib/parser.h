#pragma once
#include <string>
#include <string_view>

#include "reader.h"
#include "texp.h"

class Parser {
  Reader _r;
  Texp _char();
  Texp _string();

public:
  Parser(std::string_view literal_v);
  void whitespace();
  Texp texp();
  Texp atom();
  Texp list();
  std::string word();
  Texp file(std::string filename);

  static Texp parseTexp(std::string_view s)
    { return (Parser{s}).texp(); }
};
