#pragma once
#include "reader.hpp"
#include "texp.hpp"

#include <string>
#include <string_view>

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
