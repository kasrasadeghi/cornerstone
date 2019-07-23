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

std::optional<Texp> Matcher::sequence(Texp texp, std::vector<std::string_view> type_names, int start, int end)
  {
    CHECK(type_names.size() == end - start, "type count has to be sequence count");

    Texp proof {"sequence"};
    for (int i = start; i < end; ++i)
      {
        std::optional<Texp> result_i = is(texp[i], type_names[i - start]);
        if (result_i) 
          proof.push(*result_i);
        else 
          return std::nullopt;
      }
    return proof;
  }

std::optional<Texp> Matcher::exact(Texp texp, std::vector<std::string_view> type_names)
  {
    //TODO use sequence
    //TODO should this be a check or a false?
    if (type_names.size() != texp.size()) return std::nullopt;

    Texp proof {"exact"};

    int i = 0;
    for (auto&& type_name : type_names)
      {
        std::optional<Texp> result_i = is(texp[i++], type_name);
        if (result_i)
          proof.push(*result_i);
        else
          return std::nullopt;
      }
    return proof;
  }

/// evaluates an ordered choice between the types
std::optional<Texp> Matcher::choice(const Texp& texp, std::vector<std::string_view> type_names)
  {
    for (auto&& type_name : type_names) 
      {
        auto res = is(texp, type_name);
        if (res) 
          {
            res->value = "choice->" + res->value;
            return res;
          }
      }
    return std::nullopt;
  }

std::optional<Texp> Matcher::binop(std::string_view op, Texp t)
  { 
    if(t.value == op)
      return exact(t, {"Type", "Expr", "Expr"});
    else
      return {};
  }

std::optional<Texp> Matcher::kleene(Texp texp, std::string_view type_name, int first)
  { 
    Texp proof {"kleene"};
    for (int i = first; i < texp.size(); ++i)
      {
        std::optional<Texp> result_i = is(texp[i], type_name);
        if (not result_i) return {};
        else proof.push(*result_i);
      }
    return proof; //FIXME
  }

std::optional<Texp> Matcher::matchValue(const Texp& texp, const Texp& rule)
  {
    Texp proof {"value"};
    auto check = ([&]() {
      if (rule.value[0] == '#') 
        {
          if (rule.value == "#int")         return regexInt(texp.value);
          else if (rule.value == "#string") return regexString(texp.value);
          else if (rule.value == "#bool")   return texp.value == "true" || texp.value == "false";
          else if (rule.value == "#type")   return true; //TODO
          else if (rule.value == "#name")   return true; //TODO
          else CHECK(false, "Unmatched regex check for rule.value");
        }
      else 
        {
          return texp.value == rule.value;
        }
    })();
    if (check) 
      return Texp(rule.value);
    else
      return std::nullopt;
  }

std::optional<Texp> Matcher::matchKleene(const Texp& texp, const Texp& rule)
  {
    CHECK(rule.back().size() == 1, "A kleene star should have one element");
    std::string_view type_name = rule.back()[0].value;

    if (texp.size() < rule.size() - 1)
      return std::nullopt;

    if (rule.size() == 1)
      return kleene(texp, type_name);
    
    std::vector<std::string_view> type_names;
    type_names.reserve(rule.size() - 1);
    for (int i = 0; i < rule.size() - 1; ++i)
      type_names.emplace_back(rule[i].value);

    Texp proof {"kleene"};
    auto seq = sequence(texp, type_names, 0, rule.size() - 1);
    if (seq) 
      proof.push(*seq);
    else
      return std::nullopt;
    
    auto kle = kleene(texp, type_name, rule.size() - 1);
    if (kle)
      {
        proof.push(*kle);
        return proof;
      }
    else
      return std::nullopt;
  }

std::optional<Texp> Matcher::match(const Texp& texp, const Texp& rule)
  {
    if (rule.value[0] == '$')
      return matchFunction(texp, rule);

    auto getTypes = [&rule]() 
      {
        std::vector<std::string_view> types;
        for (auto&& c : rule) types.emplace_back(c.value);
        return std::move(types);
      };
    
    if (rule.value == "|") 
      return choice(texp, getTypes());
    
    if (not matchValue(texp, rule)) 
      return std::nullopt;

    if (not rule.empty() && rule.back().value == "*") 
      return matchKleene(texp, rule);

    return exact(texp, getTypes());
  }

//////////// function maps ////////////////////////////

std::optional<Texp> Matcher::matchFunction(const Texp& texp, const Texp& rule)
  {
    auto funcName = rule.value.substr(1);
    auto childCount = rule.size();
    std::function<std::optional<Texp>(Texp, Texp)> f = grammar_functions.at(funcName);
    return f(texp, rule);
  }

/////// Typing::is definition ///////////////

std::optional<Texp> Matcher::is(const Texp& t, std::string_view type_name)
  {
    if (auto result = match(t, grammar.getProduction(CHECK_UNWRAP(grammar.parseType(type_name), std::string(type_name) + "not in grammar"))))
      {
        result->value = std::string(type_name) + "/" + result->value;
        return result;
      }
    else
      return std::nullopt;
  }


// "Decl"       from "TopLevel/choice->Decl/exact", ::TopLevel
// "IntLiteral" from "Expr/choice->Value/choice->Literal/choice->IntLiteral/exact", ::Literal
Grammar::Type proof_type(const Grammar& g, const Texp& proof, std::string_view parent_type_name)
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