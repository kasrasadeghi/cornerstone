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
  Parser(std::string v);
  Parser(std::string_view literal_v);
  void whitespace();
  Texp texp();
  Texp atom();
  Texp list();
  std::string word();
  char operator*();
  Reader::iterator end();
  const Reader::iterator curr();
  Texp file(std::string filename);

  static Texp parseTexp(const std::string& s) 
    { return std::move((Parser{s}).texp()); }
};