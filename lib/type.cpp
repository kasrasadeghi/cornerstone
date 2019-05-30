#include "type.h"

std::ostream& Typing::operator<<(std::ostream& out, Typing::Type t) 
  { return out << getName(t); }

Typing::Type Typing::parseType(const std::string_view& s) 
  {
    auto index = std::find(type_names.begin(), type_names.end(), s);
    CHECK(index != type_names.end(), "Type from string: '" + std::string(s) + "' not found");
    return static_cast<Typing::Type>(index - type_names.begin()); 
  }