#include "grammar.h"
#include "macros.h"

#include <string>
#include <cstdlib>
#include <iostream>

using Typing::Type;
using Typing::is;


std::ostream& operator<<(std::ostream& out, Type t) 
  {
    switch(t) {
    case Type::Program:       out << "Program"; return out;
    case Type::TopLevel:      out << "TopLevel"; return out;
    case Type::StrTable:      out << "StrTable"; return out;
    case Type::StrTableEntry: out << "StrTableEntry"; return out;
    case Type::Struct:        out << "Struct"; return out;
    case Type::Field:         out << "Field"; return out;
    case Type::Def:           out << "Def"; return out;
    case Type::Decl:          out << "Decl"; return out;
    case Type::Stmt:          out << "Stmt"; return out;
    case Type::If:            out << "If"; return out;
    case Type::Store:         out << "Store"; return out;
    case Type::Auto:          out << "Auto"; return out;
    case Type::Do:            out << "Do"; return out;
    case Type::Return:        out << "Return"; return out;
    case Type::ReturnExpr:    out << "ReturnExpr"; return out;
    case Type::ReturnVoid:    out << "ReturnVoid"; return out;
    case Type::Let:           out << "Let"; return out;
    case Type::Value:         out << "Value"; return out;
    case Type::StrGet:        out << "StrGet"; return out;
    case Type::Literal:       out << "Literal"; return out;
    case Type::IntLiteral:    out << "IntLiteral"; return out;
    case Type::BoolLiteral:   out << "BoolLiteral"; return out;
    case Type::String:        out << "String"; return out;
    case Type::Name:          out << "Name"; return out;
    case Type::Types:         out << "Types"; return out;
    case Type::Type:          out << "Type"; return out;
    case Type::Params:        out << "Params"; return out;
    case Type::Param:         out << "Param"; return out;
    case Type::Expr:          out << "Expr"; return out;
    case Type::MathBinop:     out << "MathBinop"; return out;
    case Type::Icmp:          out << "Icmp"; return out;
    case Type::LT:            out << "LT"; return out;
    case Type::LE:            out << "LE"; return out;
    case Type::GT:            out << "GT"; return out;
    case Type::GE:            out << "GE"; return out;
    case Type::EQ:            out << "EQ"; return out;
    case Type::NE:            out << "NE"; return out;
    case Type::Load:          out << "Load"; return out;
    case Type::Index:         out << "Index"; return out;
    case Type::Cast:          out << "Cast"; return out;
    case Type::Add:           out << "Add"; return out;
    case Type::Call:          out << "Call"; return out;
    case Type::CallBasic:     out << "CallBasic"; return out;
    case Type::CallVargs:     out << "CallVargs"; return out;
    case Type::CallTail:      out << "CallTail"; return out;
    case Type::Args:          out << "Args"; return out;
    }
    exit(1);
  }


/// HELPERS AND COMBINATORS

auto regexInt(std::string s) -> bool 
  {
    if(s.empty() || not (isdigit(s[0]) || s[0] == '-')) return false;

    char* p;
    strtol(s.c_str(), &p, 10);

    return (*p == 0);
  }

auto regexString(std::string s) -> bool 
  {
    return not s.empty() && s.length() >= 2 && s[0] == '"' && s.back() == '"';
  }

bool exact(Texp texp, std::initializer_list<Type> types)
  {
    if (types.size() != texp.size()) return false;

    int i = 0;
    for (auto&& type : types) {
      if (not is(type, texp[i++])) return false;
    }
    return true;
  }

/// evaluates an ordered choice between the types
bool choice(Texp texp, std::initializer_list<Type> types)
  {
    for (auto&& type : types) {
      if (is(type, texp)) return true;
    }
    return false;
  }

bool binop(std::string op, Texp& t) 
  {
    using namespace Typing;
    return t.value == op
        && exact(t, {Type::Type, Type::Expr, Type::Expr});
  }

bool allChildren(Type type, Texp texp)
  { 
    for (Texp& c : texp) {
      if (not is(type, c)) return false;
    }
    return true;
  }

/// IMPLEMENTATIONS

// (ProgramName TopLevel*)
auto isProgram(Texp t) -> bool 
  { return allChildren(Type::TopLevel, t); }

// (StrTable | Struct | Def | Decl)
auto isTopLevel(Texp t) -> bool 
  { return choice(t, {Type::StrTable, Type::Struct, Type::Def, Type::Decl}); }

// (str-table StrTableEntry*)
auto isStrTable(Texp t) -> bool 
  {
    if (t.value != "str-table") return false;
    return allChildren(Type::StrTableEntry, t);
  }

// (#int #string)
auto isStrTableEntry(Texp t) -> bool 
  {
    return t.size() == 1 
        && regexInt(t.value)
        && regexString(t[0].value);
  }

// (struct Name Field*)
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

// (Name Type)
auto isField(Texp t) -> bool 
  {
    //TODO
    // - name regexp?
    // - register name/type for struct?
    return exact(t, {Type::Type});
  }

// (def FuncName Params ReturnType Do)
auto isDef(Texp t) -> bool 
  { 
    // TODO function name // TODO add to namespace functionality
    // TODO return type name // TODO add to union-find functionality
    if (t.value != "def") return false;
    return exact(t, {Type::Name, Type::Params, Type::Type, Type::Do});
  }

// (decl FuncName Types ReturnType)
auto isDecl(Texp t) -> bool 
  {
    return exact(t, {Type::Name, Type::Types, Type::Type});
  }

// (CallBasic | CallVargs | CallTail)
auto isCall(Texp t) -> bool 
  { return choice(t, {Type::CallBasic, Type::CallVargs, Type::CallTail}); }

// (call FuncName Types ReturnType Args)
auto isCallBasic(Texp t) -> bool 
  {
    // TODO function name namespace
    return t.value == "call" 
        && exact(t, {Type::Name, Type::Types, Type::Type, Type::Args});
  }
// (call-vargs FuncName Types ReturnType Args)
auto isCallVargs(Texp t) -> bool 
  {
    return t.value == "call-vargs"
        && exact(t, {Type::Name, Type::Types, Type::Type, Type::Args});
  }

// (call-tail FuncName Types ReturnType Args)
auto isCallTail(Texp t) -> bool 
  {
    return t.value == "call-tail"
        && exact(t, {Type::Name, Type::Types, Type::Type, Type::Args});
  }

// (Let | Return | If | Store | Auto | Do | Call)
auto isStmt(Texp t) -> bool 
  {
    return choice(t, {Type::Let, Type::Return, Type::If, Type::Store, Type::Auto, Type::Do, Type::Call});
  }

// (let LocalName Expr/(not Value))
auto isLet(Texp t) -> bool 
  {
    //TODO localname namespace
    return t.value == "let" 
        && exact(t, {Type::Name, Type::Expr});
  }

// (if Expr/Value Do) //TODO second do? for else branch?
auto isIf(Texp t) -> bool 
  {
    return t.value == "if"
        && exact(t, {Type::Expr, Type::Do});
  }

// (ReturnExpr | ReturnVoid)
auto isReturn(Texp t) -> bool 
  { return choice(t, {Type::ReturnExpr, Type::ReturnVoid}); }

// (return ReturnType Expr/Value)
auto isReturnExpr(Texp t) -> bool 
  {
    return t.value == "return"
        && exact(t, {Type::Expr, Type::Type});
  }

// (return-void)
auto isReturnVoid(Texp t) -> bool 
  { return t.value == "return-void" && t.empty(); }

// (store ValueExpr/Value Type LocationExpr/Value/Name/AutoName?)
auto isStore(Texp t) -> bool
  { 
    return t.value == "store"
      && exact(t, {Type::Expr, Type::Type, Type::Expr});
  }

// (auto LocalName Type)
auto isAuto(Texp t) -> bool 
  {
    // TODO local namespace
    // TODO type to allocate
    return t.value == "auto"
        && exact(t, {Type::Name, Type::Type});
  }

// (do Stmt*)
auto isDo(Texp t) -> bool {
  return t.value == "do"
      && allChildren(Type::Stmt, t);
}

// (Call | MathBinop | Icmp | Load | Index | Cast | Value)
auto isExpr(Texp t) -> bool 
  { return choice(t, {Type::Call, Type::MathBinop, Type::Icmp, Type::Load, Type::Index, Type::Cast, Type::Value}); }

// (load Type LocExpr/Value)
auto isLoad(Texp t) -> bool 
  {
    return t.value == "load"
        && exact(t, {Type::Type, Type::Expr});
  }

// (index PtrExpr Type IntExpr/IntValue)
auto isIndex(Texp t) -> bool 
  {
    return t.value == "index"
        && exact(t, {Type::Expr, Type::Type, Type::Expr});
  }

// (cast ToType FromType Expr/Value)
auto isCast(Texp t) -> bool 
  {
    return t.value == "cast"
        && exact(t, {Type::Type, Type::Type, Type::Expr});
  }

// (StrGet | Literal | Name)
auto isValue(Texp t) -> bool 
  { return choice(t, {Type::StrGet, Type::Literal, Type::Name}); }

// (IntLiteral | BoolLiteral)
auto isLiteral(Texp t) -> bool 
  { return choice(t, {Type::IntLiteral, Type::BoolLiteral}); }

auto isBoolLiteral(Texp t) -> bool
  { return (t.value == "true" || t.value == "false") && t.empty(); }

auto isIntLiteral(Texp t) -> bool 
  { return regexInt(t.value) && t.empty(); }

auto isString(Texp t) -> bool 
  { return regexString(t.value) && t.empty(); }

auto isName(Texp t) -> bool 
  { return true; /* TODO regexp or keywords or something */}

auto isTypes(Texp t) -> bool 
  { return t.value == "types" && allChildren(Type::Type, t); }

auto isType(Texp t) -> bool 
  {
    return true; //TODO namespace or primitive match
  }

// (params Param*)
auto isParams(Texp t) -> bool 
  { return t.value == "params" && allChildren(Type::Param, t); }

auto isParam(Texp t) -> bool 
  {
    //TODO t.value == name, add as parameter to closest defun ancestor
    return exact(t, {Type::Type}); 
  }

// (str-get IntLiteral)
auto isStrGet(Texp t) -> bool 
  { return t.value == "str-get" && exact(t, {Type::IntLiteral}); }

// (| Add)
auto isMathBinop(Texp t) -> bool 
  { return is(Type::Add, t); }

auto isAdd(Texp t) -> bool 
  { return binop("+", t); }

auto isIcmp(Texp t) -> bool 
  { return choice(t, {Type::LT, Type::LE, Type::GT, Type::GE, Type::EQ, Type::NE}); }
  
auto isLT(Texp t) -> bool 
  { return binop("<", t); }

auto isLE(Texp t) -> bool
  { return binop("<=", t); }

auto isGT(Texp t) -> bool
  { return binop(">", t); }

auto isGE(Texp t) -> bool
  { return binop(">=", t); }

auto isEQ(Texp t) -> bool
  { return binop("==", t); }

auto isNE(Texp t) -> bool
  { return binop("!=", t); }

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
