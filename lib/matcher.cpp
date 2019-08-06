#include "matcher.h"
#include "macros.h"
#include "parser.h"
#include "grammar.h"

#include <string>
#include <sstream>
#include <string_view>
#include <cstdlib>
#include <iostream>

////////// regex ///////////////////////////////

auto regexInt(std::string s) -> bool
  {
    if(s.empty() || not (isdigit(s[0]) || s[0] == '-')) return false;

    char* p;
    strtol(s.c_str(), &p, 10);

    return (*p == 0);
  }

auto regexString(std::string s) -> bool
  { return not s.empty() && s.length() >= 2 && s[0] == '"' && s.back() == '"'; }

/////// combinators ///////////////////////////

Texp Matcher::sequence(const Texp& texp, const Texp& type_names, int start, int end)
  {
    CHECK(type_names.size() - 1 == end - start, "length of sequence should be one less than rule for Kleene rules");

    Texp proof {"sequence"};
    for (int i = start; i < end; ++i)
      {
        Texp result_i = is(texp[i], type_names[i - start].value);
        if (result_i.value == "error")
          return result_i; // TODO consider incrementing proof
        else 
          proof.push(result_i[0]);
      }
    return {"success", {proof}};
  }

Texp Matcher::exact(const Texp& texp, const Texp& rule)
  {
    if (rule.size() != texp.size())
      return Texp("error", {Texp("\"texp does not match exact rule size\"")});

    Texp proof {"exact"};

    int i = 0;
    for (auto&& prod_name : rule)
      {
        CHECK(prod_name.size() == 0, "children of an exact sequence rule should be atomic (string only, no children)");
        Texp result_i = is(texp[i++], prod_name.value);
        if (result_i.value == "error")
          return result_i; // TODO consider pushing proof in some way
        else
          proof.push(result_i[0]);
      }
    return {"success", {proof}};
  }

/// evaluates an ordered choice between the types
Texp Matcher::choice(const Texp& texp, const Texp& rule)
  {
    for (auto&& production_name : rule)
      {
        CHECK(production_name.size() == 0, "options of a choice should have no children");
        auto res = is(texp, production_name.value);
        if (res.value == "success")
          {
            auto& result = res[0];
            result.value = "choice->" + result.value;
            return res;
          }
      }
    return {"error", {Texp("\"failed to match choice\""), rule, texp}};
  }

Texp Matcher::kleene(const Texp& texp, std::string_view type_name, int first)
  { 
    Texp proof {"kleene"};
    for (int i = first; i < texp.size(); ++i)
      {
        Texp result_i = is(texp[i], type_name);
        if (result_i.value == "error") return result_i; // TODO maybe add intermediary error reporting
        else proof.push(result_i[0]);
      }
    return {"success", {proof}};
  }

/// returns either (success value) or (error message)
/// if (success value) then you can check value.charAt(0) != '#' for keyword match
Texp Matcher::matchValue(const Texp& texp, const Texp& rule)
  {
    if (rule.value[0] == '#')
      {
        if (rule.value == "#int")
          return regexInt(texp.value) 
            ? Texp("success", {rule.value})
            : Texp{"error", {"\"'" + texp.value + "' failed to match #int\""}};

        else if (rule.value == "#string")
          return regexString(texp.value) 
            ? Texp("success", {rule.value}) 
            : Texp{"error", {"\"'" + texp.value + "' failed to match #string\""}};

        else if (rule.value == "#bool")
          return texp.value == "true" || texp.value == "false" 
            ? Texp{"success", {rule.value}} 
            : Texp{"error", {"\"'" + texp.value + "' failed to match #bool\""}};

        else if (rule.value == "#type")
          return Texp("success", {rule.value}); //TODO

        else if (rule.value == "#name")
          return Texp("success", {rule.value}); //TODO
        
        else
          CHECK(false, "\"Unmatched regex check for rule.value\"")
      }
    else 
      {
        return texp.value == rule.value
          ? Texp("success", {rule.value})
          : Texp{"error", {"\"'" + rule.value + "' keyword match failed\""}};
      }
  }

Texp Matcher::matchKleene(const Texp& texp, const Texp& rule)
  {
    CHECK(rule.back().size() == 1, "A kleene star should have one element");
    std::string_view type_name = rule.back()[0].value;

    // early exit for when texp cannot even match the non-kleene sequence
    if (texp.size() < rule.size() - 1)
      return Texp("failure", {Texp("\"failed texp.len < rule.len - 1\""), rule, texp});

    if (rule.size() == 1)
      return kleene(texp, type_name);

    Texp proof {"kleene"};
    auto seq = sequence(texp, rule, 0, rule.size() - 1);
    if (seq.value == "error")
      return seq; // TODO add kleene specific error reporting
    for (Texp child : seq[0])
      proof.push(child);
    
    auto kle = kleene(texp, type_name, rule.size() - 1);
    if (kle.value == "error")
      return kle; // TODO add kleene specific error reporting
    for (Texp child : kle[0])
      proof.push(child);

    return Texp("success", {proof});
  }

Texp Matcher::match(const Texp& texp, const Texp& rule)
  {
    if (rule.value == "|") 
      return choice(texp, rule);
    
    Texp value_result = matchValue(texp, rule);
    if (value_result.value == "error")
      return value_result;

    // TODO assert rule.size() != 0 during grammar construction
    if (rule.size() != 0 && rule.back().value == "*") 
      return matchKleene(texp, rule);

    return exact(texp, rule);
  }

/////// Typing::is definition ///////////////

Texp Matcher::is(const Texp& t, std::string_view type_name)
  {
    if (auto result = match(t, grammar.getProduction(grammar.shouldParseType(type_name))); result.value == "success")
      {
        result[0].value = std::string(type_name) + "/" + result[0].value;
        return result;
      }
    else
      return result;
  }


// "Decl"       from "TopLevel/choice->Decl/exact", ::TopLevel
// "IntLiteral" from "Expr/choice->Value/choice->Literal/choice->IntLiteral/exact", ::Literal
Grammar::Type parseChoice(const Grammar& g, const Texp& proof, std::string_view parent_type_name)
  {
    const auto& s = proof.value;

    // get the location of the Type we're choosing from
    unsigned long choice_index = s.find(parent_type_name);
    CHECK(choice_index != std::string::npos, s + " is not a choice of " + std::string(parent_type_name));
    
    std::string rest = s.substr(choice_index + parent_type_name.size());
    CHECK(rest.substr(0, 9) == "/choice->", std::string(rest.substr(7)) + " doesn't have '/choice->' after " + std::string(parent_type_name));
    
    // get the type immediately proceeding the choice
    rest = rest.substr(9);
    std::string type_name = rest.substr(0, rest.find('/'));
    return CHECK_UNWRAP(g.parseType(type_name), "child of choice in proof is not in grammar");
  }