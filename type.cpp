#include "type.h"
#include <string>
#include <cstdlib>

std::unordered_map<::Types, std::function<bool(std::string)>> literal_match {
  {Types::IntLiteral, [](std::string s){
    if(s.empty() || not (isdigit(s[0]) || s[0] == '-')) return false;

    char * p;
    strtol(s.c_str(), &p, 10);

    return (*p == 0);
  }}
};

std::unordered_map<::Types, std::function<bool(Texp)>> is {
  {Types::Program, [](Texp t){
    for (Texp& c : t) {
      bool success = is[Types::StrTable](c) || is[Types::Struct](c) || is[Types::Def](c) || is[Types::Decl](c);
      if (not success) return false;
    }
    return true;
  }},
  {Types::StrTable, [](Texp t){
    if (t.value != "str-table") return false;
    for (Texp& c : t) {
      if (not is[Types::StrTableEntry](c)) return false;
    }
    return true;
  }},
  {Types::StrTableEntry, [](Texp t){
    return t.size() == 1 
        && literal_match[Types::IntLiteral](t.value) 
        && literal_match[Types::StringLiteral](t[0].value);
  }},
  {Types::Def, [](Texp t){ 
    return t.size() == 4 
        // && is[Types::Name](t[0]) // TODO add to namespace functionality
        // && is[Types::Params](t[1])
        // && is[Types::Name](t[2]) // TODO add to union-find functionality
        // && is[Types::]
        ;
  }}
};