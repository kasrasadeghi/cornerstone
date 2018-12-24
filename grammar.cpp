#include "grammar.h"
#include "macros.h"
#include "parser.h"

#include <string>
#include <string_view>
#include <cstdlib>
#include <iostream>

using Typing::Type;
using Typing::is;

constexpr std::array<std::string_view, 46> type_names {
  "Program", 
  "TopLevel", 
  "StrTable", 
  "StrTableEntry", 
  "Struct", 
  "Field", 
  "Def", 
  "Decl", 
  "Stmt", 
  "If", 
  "Store", 
  "Auto", 
  "Do", 
  "Return", 
  "ReturnExpr", 
  "ReturnVoid", 
  "Let", 
  "Value", 
  "StrGet", 
  "Literal", 
  "IntLiteral", 
  "BoolLiteral", 
  "String", 
  "Name", 
  "Types", 
  "Type", 
  "Params", 
  "Param", 
  "Expr", 
  "MathBinop", 
  "Icmp", 
  "LT", 
  "LE", 
  "GT", 
  "GE", 
  "EQ", 
  "NE", 
  "Load", 
  "Index", 
  "Cast", 
  "Add", 
  "Call", 
  "CallBasic", 
  "CallVargs", 
  "CallTail", 
  "Args", 
};

constexpr const std::string_view& getName(Type t)
  { return type_names[static_cast<size_t>(t)]; }

std::ostream& operator<<(std::ostream& out, Type t) 
  { return out << getName(t); }

Type parseType(const std::string_view& s) 
  {
    auto index = std::find(type_names.begin(), type_names.end(), s);
    CHECK(index != type_names.end(), "Type from string: '" + string(s) + "' not found");
    return static_cast<Type>(index - type_names.begin()); 
  }

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
bool choice(Texp texp, std::vector<Type> types)
  {
    for (auto&& type : types) 
      {
        if (is(type, texp)) return true;
      }
    return false;
  }

bool binop(std::string op, Texp& t) 
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

bool match(const Texp& texp, const Texp& rule)
  {
    if (rule.value[0] == '$')
      matchFunction(texp, rule);

    auto getTypes = [&rule]() 
      {
        std::vector<Type> types;
        for (auto&& c : rule) types.emplace_back(parseType(c.value));
        return std::move(types);
      };

    if (rule.value == "|") 
      return choice(texp, getTypes());

    if (not rule.empty() && rule.back().value == "*") 
      {
        CHECK(rule.back().size() == 1, "A kleene star should have one element");
        Type type = parseType(rule.back()[0].value);

        //TODO match value for kleene star results as well

        if (not (texp.size() >= rule.size() - 1))
          return false;

        if (rule.size() == 1)           
          return kleene(texp, type);

        std::vector<Type> types;
        for (int i = 0; i < rule.size() - 1; ++i) 
          types.emplace_back(parseType(rule[i].value));

        return sequence(texp, types, 0, rule.size() - 1) 
            && kleene(texp, type, rule.size() - 1);
      }

    // check value
    if (rule.value[0] == '#') 
      {
        if (rule.value == "#int")
          {
            if (not regexInt(texp.value)) return false;
          }
        else if (rule.value == "#string")
          {
            if (not regexString(texp.value)) return false;
          }
        else if (rule.value == "#bool")
          {
            if (not (texp.value == "true" || texp.value == "false")) return false;
          }
        else if (rule.value == "#type")
          {
            // vacuous truth TODO
          }
        else if (rule.value == "#name")
          {
            // vacuous truth TODO
          }
        else
          {
            CHECK(false, "Unmatched regex check for rule.value");
          }
      }
    else 
      {
        if (texp.value != rule.value) return false;
      }

    return exact(texp, getTypes());
  }

bool match(const Texp& texp, const string& s)
  { return match(texp, Parser::parseTexp(s)); }

//////////// function maps ////////////////////////////

static std::unordered_map<std::string, std::function<bool(Texp)>> grammar_functions_unary {
  {"isProgram", [](Texp t) -> bool 
    { return kleene(t, Type::TopLevel); }},
  
  //TODO (#name Type)
  //TODO add to struct lookup
  {"isField", [](Texp t) -> bool { return exact(t, {Type::Type}); }},

  {"isBoolLiteral", [](Texp t) -> bool 
    { return (t.value == "true" || t.value == "false") && t.empty(); }},

  //TODO add as parameter to closest defun ancestor
  {"isParam", [](Texp t) -> bool { return exact(t, {Type::Type}); }},

  {"isName", [](Texp t) -> bool { return true; }},

  //TODO namespace or primitive match
  {"isType", [](Texp t) -> bool { return true; }},
};

static std::unordered_map<std::string, std::function<bool(Texp, std::string)>> grammar_functions_binary {
  {"binop", [](Texp t, std::string symbol) -> bool { return binop(symbol, t); }},  
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

bool Typing::is(Type type, const Texp& t) 
  {
    static int level = 0;
    for (int i = 0; i < level; ++i) 
      {
        std::cout << "  ";
      }
    std::cout << type << " " << t << std::endl;

    ++level;
    bool result = false;
    DEFER({
      for (int i = 0; i < level; ++i) 
        {
          std::cout << "  ";
        }
      std::cout << std::boolalpha << type << " " << result << std::endl;
    });

    DEFER( --level; );

    // DEFER({
    //   if (not result) return;
    //   for (int i = 0; i < level; ++i) {
    //     std::cout << "  ";
    //   }
    //   std::cout << type << " " << t << std::endl;
    // });

    std::string s;

    switch(type) {
    case Type::Program:       s = "$isProgram"; break; //("ProgramName" (* TopLevel))
    case Type::TopLevel:      s = "| StrTable Struct Def Decl"; break;
    case Type::StrTable:      s = "str-table (* StrTableEntry)"; break;
    case Type::StrTableEntry: s = "#int String"; break;
    case Type::Struct:        s = "struct Name (* Field)"; break;
    case Type::Field:         s = "$isField"; break; // ("FieldName" Type)
    case Type::Def:           s = "def Name Params Type Do"; break; // def FunctionName Params ReturnType Do
    case Type::Decl:          s = "decl Name Types Type"; break;
    case Type::Stmt:          s = "| Let Return If Store Auto Do Call"; break;
    case Type::If:            s = "if Expr Stmt"; break;
    case Type::Store:         s = "store Expr Type Expr"; break;
    case Type::Auto:          s = "auto Name Type"; break;
    case Type::Do:            s = "do (* Stmt)"; break;
    case Type::Return:        s = "| ReturnExpr ReturnVoid"; break;
    case Type::ReturnExpr:    s = "return Expr Type"; break;
    case Type::ReturnVoid:    s = "return-void"; break;
    case Type::Let:           s = "let Name Expr"; break;
    case Type::Value:         s = "| StrGet Literal Name"; break;
    case Type::StrGet:        s = "str-get IntLiteral"; break;
    case Type::Literal:       s = "| BoolLiteral IntLiteral"; break;
    case Type::IntLiteral:    s = "#int"; break;
    case Type::BoolLiteral:   s = "#bool"; break;
    case Type::String:        s = "#string"; break;
    case Type::Name:          s = "#name"; break;
    case Type::Types:         s = "types (* Type)"; break;
    case Type::Type:          s = "#type"; break;
    case Type::Params:        s = "params (* Param)"; break;
    case Type::Param:         s = "$isParam"; break;
    case Type::Expr:          s = "| Call MathBinop Icmp Load Index Cast Value"; break;
    case Type::MathBinop:     s = "| Add"; break;
    case Type::Icmp:          s = "| LT LE GT GE EQ NE"; break;
    case Type::LT:            s = "$binop <"; break;
    case Type::LE:            s = "$binop <="; break;
    case Type::GT:            s = "$binop >"; break;
    case Type::GE:            s = "$binop >="; break;
    case Type::EQ:            s = "$binop =="; break;
    case Type::NE:            s = "$binop !="; break;
    case Type::Load:          s = "Load Type Expr"; break;
    case Type::Index:         s = "index Expr Type Expr"; break;
    case Type::Cast:          s = "cast Type Type Expr"; break;
    case Type::Add:           s = "$binop +"; break;
    case Type::Call:          s = "| CallBasic CallVargs CallTail"; break;
    case Type::CallBasic:     s = "call Name Types Type Args"; break;
    case Type::CallVargs:     s = "call-vargs Name Types Type Args"; break;
    case Type::CallTail:      s = "call-tail Name Types Type Args"; break;
    case Type::Args:          s = "args (* Expr)"; break;
    default:  std::cout << "type not matched" << std::endl; exit(1);
    }

    return (result = match(t, "(" + s + ")"));
  }
