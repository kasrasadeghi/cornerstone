#include "grammar.h"
#include "macros.h"
#include "matcher.h"

auto operator<< (std::ostream& o, Grammar::Type t) -> std::ostream&
  { return o << t->name; }

Grammar::Grammar(Texp t)
  {
    CHECK(t.value == "Grammar", "value at root of given texp is not 'Grammar'");
    for (auto& child : t)
      {
        CHECK(0 != child.size(), "a production of a grammar should not be empty");
        types.emplace_back(child.value, child[0]);
      }
  }

std::optional<Grammar::Type> Grammar::parseType(std::string_view s) const
  {
    auto iter = std::find_if(types.cbegin(), types.cend(), [s](const TypeRecord& tr) { return tr.name == s; });
    if (iter != types.cend())
      return iter;
    else
      return std::nullopt;
  }

Grammar::Type Grammar::shouldParseType(std::string_view s) const
  {
    auto iter = std::find_if(types.cbegin(), types.cend(), [s](const TypeRecord& tr) { return tr.name == s; });
    CHECK(iter != types.cend(), std::string(s) + " not in grammar");
    return iter;
  }

const Texp& Grammar::getProduction(Type type) const
  { return type->production; }

std::optional<std::string_view> Grammar::getKeyword(std::string_view s) const
  {
    Type type = shouldParseType(s);
    if (type->production.value.starts_with("#") || type->production.value == "|")
      return std::nullopt;
    else
      return type->production.value;
  }

void UnionMatch(const Grammar& g,
                std::string_view parent_type_name,
                const Texp& texp, 
                const Texp& proof,
                std::vector<std::pair<std::string_view, std::function<void(const Texp&, const Texp&)>>> cases, 
                bool exhaustive)
  {
    CHECK(g.parseType(parent_type_name), "parent choice '" + std::string(parent_type_name) + "' not in grammar")
    Grammar::Type texp_type = parseChoice(g, proof, parent_type_name);
    for (auto& [case_name, case_f] : cases) 
      {
        Grammar::Type case_type = CHECK_UNWRAP(g.parseType(case_name), "case '" + std::string(case_name) + "' not in grammar.");
        if (case_type == texp_type)
          {
            case_f(texp, proof);
            return;
          }
      }
    CHECK(not exhaustive, texp_type->name + " is unhandled in " + std::string(parent_type_name) + "()'s type switch");
  }