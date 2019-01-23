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

bool sequence(Texp texp, std::vector<Type> types, int start, int end)
  {
    CHECK(types.size() == end - start, "type count has to be sequence count");
    for (int i = start; i < end; ++i)
      {
        if (not is(types[i - start], texp[i])) return false;
      }
    return true;
  }

bool exact(Texp texp, std::vector<Type> types)
  {
    //TODO use sequence
    if (types.size() != texp.size()) return false;

    int i = 0;
    for (auto&& type : types)
      {
        if (not is(type, texp[i++])) return false;
      }
    return true;
  }

/// evaluates an ordered choice between the types
bool choice(const Texp& texp, std::vector<Type> types)
  {
    auto& output = is_buffer;
    std::cout << output.str();
    output.clear();
    output.str(std::string());
    
    for (auto&& type : types) 
      {
        if (is(type, texp))
	  {
	    std::cout << output.str();
	    output.clear();
	    output.str(std::string());
	    return true;
	  }
	else
	  {
	    output.clear();
	    output.str(std::string());
	  }
      }
    return false;
  }

bool binop(std::string_view op, Texp t)
  { return t.value == op && exact(t, {Type::Type, Type::Expr, Type::Expr}); }

bool kleene(Texp texp, Type type, int first = 0)
  { 
    for (int i = first; i < texp.size(); ++i)
      {
        if (not is(type, texp[i])) return false;
      }
    return true;
  }

bool matchFunction(const Texp& texp, const Texp& rule);

bool matchValue(const Texp& texp, const Texp& rule)
  {
    if (rule.value[0] == '#') 
      {
        if (rule.value == "#int") return regexInt(texp.value);
        else if (rule.value == "#string") return regexString(texp.value);
        else if (rule.value == "#bool") return texp.value == "true" || texp.value == "false";
        else if (rule.value == "#type") return true; //TODO
        else if (rule.value == "#name") return true; //TODO
        else CHECK(false, "Unmatched regex check for rule.value");
      }
    else 
      {
        return texp.value == rule.value;
      }
  }

bool matchKleene(const Texp& texp, const Texp& rule)
  {
    CHECK(rule.back().size() == 1, "A kleene star should have one element");
    Type type = Typing::parseType(rule.back()[0].value);

    if (texp.size() < rule.size() - 1)
      return false;

    if (rule.size() == 1)
      return kleene(texp, type);


    std::vector<Type> types;
    types.reserve(rule.size() - 1);
    for (int i = 0; i < rule.size() - 1; ++i)
      types.emplace_back(Typing::parseType(rule[i].value));

    return sequence(texp, types, 0, rule.size() - 1) 
        && kleene(texp, type, rule.size() - 1);
  }

bool match(const Texp& texp, const Texp& rule)
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
      return false;

    if (not rule.empty() && rule.back().value == "*") 
      return matchKleene(texp, rule);

    return exact(texp, getTypes());
  }

bool match(const Texp& texp, std::string_view s)
  { return match(texp, Parser::parseTexp(s)); }

//////////// function maps ////////////////////////////

static std::unordered_map<std::string, std::function<bool(Texp, Texp)>> grammar_functions {
  {"binop", [](Texp t, Texp rule) -> bool { std::string_view symbol = rule[0].value; return binop(symbol, t); }},
};

bool matchFunction(const Texp& texp, const Texp& rule)
  {
    auto funcName = rule.value.substr(1);
    auto childCount = rule.size();
    std::function<bool(Texp, Texp)> f = grammar_functions.at(funcName);
    return f(texp, rule);
  }

/////// big Typing::is definition ///////////////

// bool Typing::is(Type type, const Texp& t, bool trace) 
//   {
//     static int level = 0;

//     if (trace)
//       {
//         for (int i = 0; i < level; ++i) 
//           std::cout << "  ";
//         std::cout << type << " " << t << std::endl;
//       }

//     std::string_view s = Grammar::getProduction(type);

//     ++level;
//     bool result = match(t, s);

//     if (not trace && result)
//       {
//         for (int i = 0; i < level; ++i)
//           std::cout << "| ";
//         std::cout << type << " " << t << std::endl;
//       }
//     --level;

//     if (trace)
//       {
//         for (int i = 0; i < level; ++i) 
//           std::cout << "  ";
//         std::cout << std::boolalpha << type << " " << result << std::endl;
//       }
//     return result;
//   }

bool Typing::is(Type type, const Texp& t)
  {
    using std::cout;
    static int level = 0;

    ++level;
    auto result = match(t, Grammar::getProduction(type));
    --level;

    for (int i = 0; i < level; ++i) 
      is_buffer << "  ";
    is_buffer << type << " " << t << "\n";
    
    return result;
  }

