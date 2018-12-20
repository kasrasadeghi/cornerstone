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
  { return static_cast<Type>(std::find(type_names.begin(), type_names.end(), s) - type_names.begin()); }

/// HELPERS AND COMBINATORS

auto regexInt(std::string s) -> bool 
  {
    if(s.empty() || not (isdigit(s[0]) || s[0] == '-')) return false;

    char* p;
    strtol(s.c_str(), &p, 10);

    return (*p == 0);
  }

auto regexString(std::string s) -> bool
  { return not s.empty() && s.length() >= 2 && s[0] == '"' && s.back() == '"'; }

bool exact(Texp texp, std::vector<Type> types)
  {
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

bool allChildren(Type type, Texp texp)
  { 
    for (Texp& c : texp) 
      {
        if (not is(type, c)) return false;
      }
    return true;
  }

bool match(Texp texp, Texp rule)
  {
    auto getTypes = [&rule]() 
      {
        std::vector<Type> types;
        for (auto&& c : rule) types.emplace_back(parseType(c.value));
        return std::move(types);
      };

    if (rule.value == "|") 
      return choice(texp, getTypes());

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

bool match(Texp texp, const string& s)
  { return match(texp, Parser::parseTexp(s)); }

/// IMPLEMENTATIONS

// ("ProgramName" (* TopLevel))
// ($isProgram)
auto isProgram(Texp t) -> bool 
  { return allChildren(Type::TopLevel, t); }

// (| StrTable Struct Def Decl)
auto isTopLevel(Texp t) -> bool 
  { return match(t, "(| StrTable Struct Def Decl)"); }

// (str-table (* StrTableEntry))
auto isStrTable(Texp t) -> bool 
  { return t.value == "str-table" && allChildren(Type::StrTableEntry, t); }

// (#int String)
auto isStrTableEntry(Texp t) -> bool 
  { return match(t, "(#int String)"); }

// (struct Name (* Field))
// ($isStruct)
auto isStruct(Texp t) -> bool 
  {
    //TODO allChildren offset or just general match mechanism
    if (not (t.value == "struct"
        && t.size() >= 2 //TODO in llvm, are there semantics for empty structs?
        && is(Type::Name, t[0]) //TODO struct namespace and type namespace
        )) return false;
    for (int i = 1; i < t.size(); ++i) 
      {
        if (not is(Type::Field, t[i])) return false;
      }
    return true;
  }

// ("Name" Type)
// ($isField)
auto isField(Texp t) -> bool 
    //TODO
    // - name regexp?
    // - register name/type for struct?
  { return exact(t, {Type::Type}); }

// (def FuncName Params ReturnType Do)
auto isDef(Texp t) -> bool 
    // TODO return type name // TODO add to union-find functionality
    // TODO function name // TODO add to namespace functionality
  { return match(t, "(def Name Params Type Do)"); }

// (decl FuncName Types ReturnType)
auto isDecl(Texp t) -> bool
  { return match(t, "(decl Name Types Type)"); }

// (| CallBasic CallVargs CallTail)
auto isCall(Texp t) -> bool 
  { return match(t, "(| CallBasic CallVargs CallTail)"); }

// (call FuncName Types ReturnType Args)
auto isCallBasic(Texp t) -> bool 
    // TODO function name namespace
  { return match(t, "(call Name Types Type Args)"); }
  
// (call-vargs FuncName Types ReturnType Args)
auto isCallVargs(Texp t) -> bool 
  { return match(t, "(call-vargs Name Types Type Args)"); }

// (call-tail FuncName Types ReturnType Args)
auto isCallTail(Texp t) -> bool 
  { return match(t, "(call-tail Name Types Type Args)"); }

// (| Let Return If Store Auto Do Call)
auto isStmt(Texp t) -> bool 
  { return match(t, "(| Let Return If Store Auto Do Call)"); }

// (let LocalName Expr/(not Value))
auto isLet(Texp t) -> bool 
    //TODO localname namespace
  { return match(t, "(let Name Expr)"); }  

// (if Expr/Value Do) //TODO second do? for else branch?
auto isIf(Texp t) -> bool 
  { return match(t, "(If Expr Do)"); }  

// (| ReturnExpr ReturnVoid)
auto isReturn(Texp t) -> bool
  { return match(t, "(| ReturnExpr ReturnVoid)"); }

// (return Expr/Value ReturnType)
auto isReturnExpr(Texp t) -> bool
  { return match(t, "(return Expr Type)"); }

// (return-void)
auto isReturnVoid(Texp t) -> bool
  { return match(t, "(return-void)"); }

// (store ValueExpr/Value Type LocationExpr/Value/Name/AutoName?)
auto isStore(Texp t) -> bool
  { return match(t, "(store Expr Type Expr)"); }

// (auto LocalName Type)
auto isAuto(Texp t) -> bool 
    // TODO local namespace
    // TODO type to allocate
  { return match(t, "(auto Name Type)"); }

// (do (* Stmt))
auto isDo(Texp t) -> bool 
  { return t.value == "do" && allChildren(Type::Stmt, t); }

// (| Call MathBinop Icmp Load Index Cast Value)
auto isExpr(Texp t) -> bool 
  { return match(t, "(| Call MathBinop Icmp Load Index Cast Value)"); }

// (load Type LocExpr/Value)
auto isLoad(Texp t) -> bool 
  { return match(t, "(load Type Expr)"); }

// (index PtrExpr Type IntExpr/IntValue)
auto isIndex(Texp t) -> bool 
  { return match(t, "(index Expr Type Expr)"); }

// (cast ToType FromType Expr/Value)
auto isCast(Texp t) -> bool 
  { return match(t, "(cast Type Type Expr)"); }

// (| StrGet Literal Name)
auto isValue(Texp t) -> bool 
  { return match(t, "(| StrGet Literal Name)"); }

// (| IntLiteral BoolLiteral)
auto isLiteral(Texp t) -> bool 
  { return match(t, "(| IntLiteral BoolLiteral)"); }

// (#bool)
auto isBoolLiteral(Texp t) -> bool
  { return (t.value == "true" || t.value == "false") && t.empty(); }

// (#int)
auto isIntLiteral(Texp t) -> bool 
  { return match(t, "(#int)"); }

// (#string)
auto isString(Texp t) -> bool 
  { return match(t, "(#string)"); }

// ($isName)
auto isName(Texp t) -> bool 
  { return true; /* TODO regexp or keywords or something */}

// (types (* Type))
auto isTypes(Texp t) -> bool 
  { return t.value == "types" && allChildren(Type::Type, t); }

// ($isType)
auto isType(Texp t) -> bool 
    //TODO namespace or primitive match
  { return true; }

// (params (* Param))
auto isParams(Texp t) -> bool
  { return t.value == "params" && allChildren(Type::Param, t); }

//TODO t.value == name
// (Name Type)
// ($isParam)
auto isParam(Texp t) -> bool 
  //TODO add as parameter to closest defun ancestor
  { return exact(t, {Type::Type}); }

// (str-get IntLiteral)
auto isStrGet(Texp t) -> bool
  { return match(t, "(str-get IntLiteral)"); }

// (| Add)
auto isMathBinop(Texp t) -> bool 
  { return match(t, "(| Add)"); }

// ($binop +)
auto isAdd(Texp t) -> bool 
  { return binop("+", t); }

// (| LT LE GT GE EQ NE)
auto isIcmp(Texp t) -> bool 
  { return match(t, "(| LT LE GT GE EQ NE)"); }
  
// ($binop -)
auto isLT(Texp t) -> bool 
  { return binop("<", t); }

// ($binop <=)
auto isLE(Texp t) -> bool
  { return binop("<=", t); }

// ($binop >)
auto isGT(Texp t) -> bool
  { return binop(">", t); }

// ($binop >=)
auto isGE(Texp t) -> bool
  { return binop(">=", t); }

// ($binop ==)
auto isEQ(Texp t) -> bool
  { return binop("==", t); }

// ($binop !=)
auto isNE(Texp t) -> bool
  { return binop("!=", t); }

// (args (* Expr))
auto isArgs(Texp t) -> bool 
  { return t.value == "args" && allChildren(Type::Expr, t); }

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

    switch(type) {
    case Type::Program:       result = isProgram(t); break;
    case Type::TopLevel:      result = isTopLevel(t); break;
    case Type::StrTable:      result = isStrTable(t); break;
    case Type::StrTableEntry: result = isStrTableEntry(t); break;
    case Type::Struct:        result = isStruct(t); break;
    case Type::Field:         result = isField(t); break;
    case Type::Def:           result = isDef(t); break;
    case Type::Decl:          result = isDecl(t); break;
    case Type::Stmt:          result = isStmt(t); break;
    case Type::If:            result = isIf(t); break;
    case Type::Store:         result = isStore(t); break;
    case Type::Auto:          result = isAuto(t); break;
    case Type::Do:            result = isDo(t); break;
    case Type::Return:        result = isReturn(t); break;
    case Type::ReturnExpr:    result = isReturnExpr(t); break;
    case Type::ReturnVoid:    result = isReturnVoid(t); break;
    case Type::Let:           result = isLet(t); break;
    case Type::Value:         result = isValue(t); break;
    case Type::StrGet:        result = isStrGet(t); break;
    case Type::Literal:       result = isLiteral(t); break;
    case Type::IntLiteral:    result = isIntLiteral(t); break;
    case Type::BoolLiteral:   result = isBoolLiteral(t); break;
    case Type::String:        result = isString(t); break;
    case Type::Name:          result = isName(t); break;
    case Type::Types:         result = isTypes(t); break;
    case Type::Type:          result = isType(t); break;
    case Type::Params:        result = isParams(t); break;
    case Type::Param:         result = isParam(t); break;
    case Type::Expr:          result = isExpr(t); break;
    case Type::MathBinop:     result = isMathBinop(t); break;
    case Type::Icmp:          result = isIcmp(t); break;
    case Type::LT:            result = isLT(t); break;
    case Type::LE:            result = isLE(t); break;
    case Type::GT:            result = isGT(t); break;
    case Type::GE:            result = isGE(t); break;
    case Type::EQ:            result = isEQ(t); break;
    case Type::NE:            result = isNE(t); break;
    case Type::Load:          result = isLoad(t); break;
    case Type::Index:         result = isIndex(t); break;
    case Type::Cast:          result = isCast(t); break;
    case Type::Add:           result = isAdd(t); break;
    case Type::Call:          result = isCall(t); break;
    case Type::CallBasic:     result = isCallBasic(t); break;
    case Type::CallVargs:     result = isCallVargs(t); break;
    case Type::CallTail:      result = isCallTail(t); break;
    case Type::Args:          result = isArgs(t); break;

    default: std::cout << "type not matched" << std::endl; exit(1);
    }
    return result;
  }
