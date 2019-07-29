#pragma once
#include "parser.h"
#include "macros.h"
#include <string_view>
#include <optional>
#include <functional>

class Grammar {
public:
  class TypeRecord {
  public:
  std::string name;
  Texp production;
  TypeRecord(std::string n, Texp p): name(n), production(p) {}
  };

std::vector<TypeRecord> types;

using Type = std::vector<TypeRecord>::const_iterator;

Grammar(Texp t);
std::optional<Type> parseType(std::string_view s) const;
Type shouldParseType(std::string_view s) const;
const Texp& getProduction(Type type);
};

void UnionMatch(const Grammar& g,
                std::string_view parent_type_name, 
                const Texp& texp, 
                const Texp& proof,
                std::vector<std::pair<std::string_view, std::function<void(const Texp&, const Texp&)>>> cases,
                bool exhaustive = true);
  