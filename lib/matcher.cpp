#include "matcher.h"
#include "macros.h"
#include "parser.h"
#include "grammar.h"

#include <string>
#include <sstream>
#include <string_view>
#include <cstdlib>
#include <iostream>

using Typing::Type;
using Typing::is;


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

std::optional<Texp> sequence(Texp texp, std::vector<Type> types, int start, int end)
  {
    CHECK(types.size() == end - start, "type count has to be sequence count");

    Texp proof {"sequence"};
    for (int i = start; i < end; ++i)
      {
        std::optional<Texp> result_i = is(types[i - start], texp[i]);
        if (result_i) 
          proof.push(*result_i);
        else 
          return std::nullopt;
      }
    return proof;
  }

std::optional<Texp> exact(Texp texp, std::vector<Type> types)
  {
    //TODO use sequence
    //TODO should this be a check or a false?
    if (types.size() != texp.size()) return std::nullopt;

    Texp proof {"exact"};

    int i = 0;
    for (auto&& type : types)
      {
        std::optional<Texp> result_i = is(type, texp[i++]);
        if (result_i)
          proof.push(*result_i);
        else
          return std::nullopt;
      }
    return proof;
  }

/// evaluates an ordered choice between the types
std::optional<Texp> choice(const Texp& texp, std::vector<Type> types)
  {
    for (auto&& type : types) 
      {
        auto res = is(type, texp);
        if (res) 
          {
            res->value = "choice->" + res->value;
            return res;
          }
      }
    return std::nullopt;
  }

std::optional<Texp> binop(std::string_view op, Texp t)
  { 
    if(t.value == op)
      return exact(t, {Type::Type, Type::Expr, Type::Expr});
    else
      return {};
  }

std::optional<Texp> kleene(Texp texp, Type type, int first = 0)
  { 
    Texp proof {"kleene"};
    for (int i = first; i < texp.size(); ++i)
      {
        std::optional<Texp> result_i = is(type, texp[i]);
        if (not result_i) return {};
        else proof.push(*result_i);
      }
    return proof; //FIXME
  }

std::optional<Texp> matchFunction(const Texp& texp, const Texp& rule);

std::optional<Texp> matchValue(const Texp& texp, const Texp& rule)
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

std::optional<Texp> matchKleene(const Texp& texp, const Texp& rule)
  {
    CHECK(rule.back().size() == 1, "A kleene star should have one element");
    Type type = Typing::parseType(rule.back()[0].value);

    if (texp.size() < rule.size() - 1)
      return std::nullopt;

    if (rule.size() == 1)
      return kleene(texp, type);


    std::vector<Type> types;
    types.reserve(rule.size() - 1);
    for (int i = 0; i < rule.size() - 1; ++i)
      types.emplace_back(Typing::parseType(rule[i].value));

    Texp proof {"kleene"};
    auto seq = sequence(texp, types, 0, rule.size() - 1);
    if (seq) 
      proof.push(*seq);
    else
      return std::nullopt;
    auto kle = kleene(texp, type, rule.size() - 1);
    if (kle)
      {
        proof.push(*kle);
        return proof;
      }
    else
      return std::nullopt;
  }

std::optional<Texp> match(const Texp& texp, const Texp& rule)
  {
    if (rule.value[0] == '$')
      return matchFunction(texp, rule);

    auto getTypes = [&rule]() 
      {
        std::vector<Type> types;
        for (auto&& c : rule) types.emplace_back(Typing::parseType(c.value));
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

std::optional<Texp> match(const Texp& texp, std::string_view s)
  { return match(texp, Parser::parseTexp(s)); }

//////////// function maps ////////////////////////////

static std::unordered_map<std::string, std::function<std::optional<Texp>(Texp, Texp)>> grammar_functions {
  {"binop", [](Texp t, Texp rule) -> std::optional<Texp> { std::string_view symbol = rule[0].value; return binop(symbol, t); }},
};

std::optional<Texp> matchFunction(const Texp& texp, const Texp& rule)
  {
    auto funcName = rule.value.substr(1);
    auto childCount = rule.size();
    std::function<std::optional<Texp>(Texp, Texp)> f = grammar_functions.at(funcName);
    return f(texp, rule);
  }

/////// Typing::is definition ///////////////

std::optional<Texp> Typing::is(Type type, const Texp& t)
  {
    if (auto result = match(t, Grammar::getProduction(type)))
      {
        result->value = std::string(getName(type)) + "/" + result->value;
        return result;
      }
    else
      return std::nullopt;
  }

