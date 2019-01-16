#include "grammar.h"
#include "macros.h"
#include "parser.h"

#include <string>
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
    for (auto&& type : types) 
      {
        if (is(type, texp)) return true;
      }
    return false;
  }

bool binop(const std::string& op, Texp& t)
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

static std::unordered_map<std::string, std::function<bool(Texp)>> grammar_functions_unary {
};

static std::unordered_map<std::string, std::function<bool(Texp, const std::string&)>> grammar_functions_binary {
  {"binop", [](Texp t, const std::string& symbol) -> bool { return binop(symbol, t); }},
};

bool matchFunction(const Texp& texp, const Texp& rule)
  {
    auto funcName = rule.value.substr(1);
    auto childCount = rule.size();
    if (childCount == 0)
      {
        auto f = grammar_functions_unary.at(funcName);
        return f(texp);
      }
    else if (childCount == 1)
      {
        auto f = grammar_functions_binary.at(funcName);
        return f(texp, rule[0].value);
      }
    else
      {
        CHECK(false, "A matching function should have had either 0 or 1 children");
      }
  }


/////// big Typing::is definition ///////////////

// static std::unordered_map<std::string, 

bool Typing::is(Type type, const Texp& t, bool trace) 
  {
    static int level = 0;

    if (trace)
      {
        for (int i = 0; i < level; ++i) 
          std::cout << "  ";
        std::cout << type << " " << t << std::endl;
      }

    std::string_view s;
    switch(type) {
    case Type::Program:       s = "(#name (* TopLevel))"; break;
    case Type::TopLevel:      s = "(| StrTable Struct Def Decl)"; break;
    case Type::StrTable:      s = "(str-table (* StrTableEntry))"; break;
    case Type::StrTableEntry: s = "(#int String)"; break;
    case Type::Struct:        s = "(struct Name (* Field))"; break;
    case Type::Field:         s = "(#name Type)"; break;
    case Type::Def:           s = "(def Name Params Type Do)"; break;
    case Type::Decl:          s = "(decl Name Types Type)"; break;
    case Type::Stmt:          s = "(| Let Return If Store Auto Do Call)"; break;
    case Type::If:            s = "(if Expr Stmt)"; break;
    case Type::Store:         s = "(store Expr Type Expr)"; break;
    case Type::Auto:          s = "(auto Name Type)"; break;
    case Type::Do:            s = "(do (* Stmt))"; break;
    case Type::Return:        s = "(| ReturnExpr ReturnVoid)"; break;
    case Type::ReturnExpr:    s = "(return Expr Type)"; break;
    case Type::ReturnVoid:    s = "(return-void)"; break;
    case Type::Let:           s = "(let Name Expr)"; break;
    case Type::Value:         s = "(| StrGet Literal Name)"; break;
    case Type::StrGet:        s = "(str-get IntLiteral)"; break;
    case Type::Literal:       s = "(| BoolLiteral IntLiteral)"; break;
    case Type::IntLiteral:    s = "(#int)"; break;
    case Type::BoolLiteral:   s = "(#bool)"; break;
    case Type::String:        s = "(#string)"; break;
    case Type::Name:          s = "(#name)"; break;
    case Type::Types:         s = "(types (* Type))"; break;
    case Type::Type:          s = "(#type)"; break;
    case Type::Params:        s = "(params (* Param))"; break;
    case Type::Param:         s = "(#name Type)"; break;
    case Type::Expr:          s = "(| Call MathBinop Icmp Load Index Cast Value)"; break;
    case Type::MathBinop:     s = "(| Add)"; break;
    case Type::Icmp:          s = "(| LT LE GT GE EQ NE)"; break;
    case Type::LT:            s = "($binop <)"; break;
    case Type::LE:            s = "($binop <=)"; break;
    case Type::GT:            s = "($binop >)"; break;
    case Type::GE:            s = "($binop >=)"; break;
    case Type::EQ:            s = "($binop ==)"; break;
    case Type::NE:            s = "($binop !=)"; break;
    case Type::Load:          s = "(load Type Expr)"; break;
    case Type::Index:         s = "(index Expr Type Expr)"; break;
    case Type::Cast:          s = "(cast Type Type Expr)"; break;
    case Type::Add:           s = "($binop +)"; break;
    case Type::Call:          s = "(| CallBasic CallVargs CallTail)"; break;
    case Type::CallBasic:     s = "(call Name Types Type Args)"; break;
    case Type::CallVargs:     s = "(call-vargs Name Types Type Args)"; break;
    case Type::CallTail:      s = "(call-tail Name Types Type Args)"; break;
    case Type::Args:          s = "(args (* Expr))"; break;
    default:  std::cout << "type not matched" << std::endl; exit(1);
    }

    ++level;
    bool result = match(t, s);

    if (not trace && result)
      {
        for (int i = 0; i < level; ++i)
          std::cout << "| ";
        std::cout << type << " " << t << std::endl;
      }
    --level;

    if (trace)
      {
        for (int i = 0; i < level; ++i) 
          std::cout << "  ";
        std::cout << std::boolalpha << type << " " << result << std::endl;
      }
    return result;
  }
