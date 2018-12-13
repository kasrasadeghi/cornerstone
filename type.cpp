#include "type.h"
#include <string>
#include <cstdlib>

using Typing::Type;
using Typing::is;

auto regexInt(std::string s) -> bool {
  if(s.empty() || not (isdigit(s[0]) || s[0] == '-')) return false;

  char* p;
  strtol(s.c_str(), &p, 10);

  return (*p == 0);
}
auto regexString(std::string s) -> bool {
  return not s.empty() && s.length() >= 2 && s[0] == '"' && s.back() == '"';
}

bool binop(std::string op, Texp& t);

// (ProgramName TopLevel*)
auto isProgram(Texp t) -> bool {
  for (Texp& c : t) {
    if (not is(Type::TopLevel, c)) return false;
  }
  return true;
}

// (StrTable || Struct || Def || Decl)
auto isTopLevel(Texp t) -> bool {
  return is(Type::StrTable, t) || is(Type::Struct, t) || is(Type::Def, t) || is(Type::Decl, t);
}

// (str-table StrTableEntry*)
auto isStrTable(Texp t) -> bool {
  if (t.value != "str-table") return false;
  for (Texp& c : t) {
    if (not is(Type::StrTableEntry, c)) return false;
  }
  return true;
}
// (#int #string)
auto isStrTableEntry(Texp t) -> bool {
  return t.size() == 1 
      && regexInt(t.value)
      && regexString(t[0].value);
}

// (struct Name Field*)
auto isStruct(Texp t) -> bool {
  if (not (t.value == "struct"
      && t.size() >= 2 //TODO in llvm, are there semantics for empty structs?
      && is(Type::Name, t[0]) //TODO struct namespace and type namespace
      )) return false;
  for (int i = 1; i < t.size(); ++i) {
    if (not is(Type::Field, t[i])) return false;
  }
  return true;
}

auto isField(Texp t) -> bool {
  return //TODO name regexp?
         t.size() == 1
      && is(Type::Type, t[0]);
}

// (def FuncName Params ReturnType Do)
auto isDef(Texp t) -> bool { 
  if (t.value != "def") return false;
  return t.size() == 4 
      && is(Type::Name, t[0]) // TODO function name // TODO add to namespace functionality
      && is(Type::Params, t[1])
      && is(Type::Type, t[2]) // TODO return type name // TODO add to union-find functionality
      && is(Type::Do, t[3]);
}
// (decl FuncName Types ReturnType)
auto isDecl(Texp t) -> bool {
  return t.size() == 3
      && is(Type::Name, t[0]) 
      && is(Type::Types, t[1])
      && is(Type::Type, t[2]);
}
// (CallBasic | CallVargs | CallTail)
auto isCall(Texp t) -> bool {
  return is(Type::CallBasic, t) || is(Type::CallVargs, t) || is(Type::CallTail, t);
}
// (call FuncName Types ReturnType Args)
auto isCallBasic(Texp t) -> bool {
  if (t.value != "call") return false;
  return t.size() == 4
      && is(Type::Name, t[0]) // TODO function name
      && is(Type::Types, t[1])
      && is(Type::Type, t[2]) // TODO return type
      && is(Type::Args, t[3])
      ;
}
// (call-vargs FuncName Types ReturnType Args)
auto isCallVargs(Texp t) -> bool {
  if (t.value != "call") return false;
  return t.size() == 4
      && is(Type::Name, t[0]) // TODO function name
      && is(Type::Types, t[1])
      && is(Type::Type, t[2]) // TODO return type
      && is(Type::Args, t[3])
      ;
}
// (call-tail FuncName Types ReturnType Args)
auto isCallTail(Texp t) -> bool {
  if (t.value != "call-stmt") return false;
  return t.size() == 4
      && is(Type::Name, t[0]) // TODO function name
      && is(Type::Types, t[1])
      && is(Type::Type, t[2]) // TODO return type
      && is(Type::Args, t[3])
      ;
}
// (Let | Return | If | Store | Auto | Do | Call)
auto isStmt(Texp t) -> bool {
  return is(Type::Stmt, t) 
      || is(Type::Return, t) 
      || is(Type::If, t)
      || is(Type::Store, t)
      || is(Type::Auto, t)
      || is(Type::Do, t)
      || is(Type::Call, t)
  ;
}
// (let LocalName Expr/(not Value))
auto isLet(Texp t) -> bool {
  if (t.value != "let") return false;
  return t.size() == 2
      && is(Type::Name, t[0])
      && is(Type::Expr, t[1])
      ;
}
// (if Expr/Value Do) //TODO second do? for else branch?
auto isIf(Texp t) -> bool {
  if (t.value != "if") return false;
  return t.size() == 2
      && is(Type::Expr, t[0])
      && is(Type::Do, t[1]);
}

// (ReturnExpr || ReturnVoid)
auto isReturn(Texp t) -> bool {
  return is(Type::ReturnExpr, t) || is(Type::ReturnVoid, t);
}

// (return ReturnType Expr/Value)
auto isReturnExpr(Texp t) -> bool {
  if (t.value != "return") return false;
  return t.size() == 2
      && is(Type::Type, t[0])
      && is(Type::Expr, t[1]);
}
// (return-void)
auto isReturnVoid(Texp t) -> bool {
  return t.value == "return-void" && t.size() == 0;
}
// (store ValueExpr/Value Type LocationExpr/Value/Name/AutoName?)
auto isStore(Texp t) -> bool {
  return t.value == "store"
      && t.size() == 3
      && is(Type::Expr, t[0])
      && is(Type::Type, t[1])
      && is(Type::Expr, t[2]);
}
// (auto LocalName Type)
auto isAuto(Texp t) -> bool {
  return t.value == "auto"
      && t.size() == 2
      && is(Type::Name, t[0]) // TODO local namespace
      && is(Type::Type, t[1]) // TODO type to allocate
      ;
}
// (do Stmt*)
auto isDo(Texp t) -> bool {
  if (t.value != "do") return false;
  for (auto& c : t) {
    if (not is(Type::Stmt, c)) return false;
  }
  return true;
}
// (Call | MathBinop | Icmp | Load | Index | Cast)
auto isExpr(Texp t) -> bool {
  return is(Type::Call, t) 
      || is(Type::MathBinop, t) 
      || is(Type::Icmp, t)
      || is(Type::Load, t)
      || is(Type::Index, t)
      || is(Type::Cast, t)
      ;
}
// (load Type LocExpr/Value)
auto isLoad(Texp t) -> bool {
  return t.value == "load"
      && t.size() == 2
      && is(Type::Type, t[0])
      && is(Type::Expr, t[1]);
}
// (index PtrExpr Type IntExpr/IntValue)
auto isIndex(Texp t) -> bool {
  return t.value == "index"
      && t.size() == 3
      && is(Type::Expr, t[0])
      && is(Type::Type, t[1])
      && is(Type::Expr, t[2]);
}
// (cast ToType FromType Expr/Value)
auto isCast(Texp t) -> bool {
  return t.value == "cast"
      && t.size() == 3
      && is(Type::Type, t[0])
      && is(Type::Type, t[1])
      && is(Type::Expr, t[2]);
}
// (StrGet | Literal | Name)
auto isValue(Texp t) -> bool {
  return is(Type::StrGet, t) || is(Type::Literal, t) || is(Type::Name, t);
}
// (IntLiteral | BoolLiteral)
auto isLiteral(Texp t) -> bool {
  return is(Type::IntLiteral, t) || is(Type::BoolLiteral, t);
}
auto isBoolLiteral(Texp t) -> bool {
  return (t.value == "true" || t.value == "false") && t.size() == 0;
}
auto isIntLiteral(Texp t) -> bool {
  return regexInt(t.value) && t.size() == 0;
}
auto isString(Texp t) -> bool {
  return regexString(t.value) && t.size() == 0;
}

auto isName(Texp t) -> bool {
  return true; //TODO regexp or keywords or something
}

auto isTypes(Texp t) -> bool {
  if (not (t.value == "types")) return false;
  
  for (int i = 0; i < t.size(); ++i) {
    if (not is(Type::Type, t[i])) return false;
  }
  return true;
}

auto isType(Texp t) -> bool {
  return true; //TODO namespace or primitive match
}

auto isParams(Texp t) -> bool {
  if (not (t.value == "params")) return false;

  for (int i = 0; i < t.size(); ++i) {
    if (not is(Type::Param, t[i])) return false;
  }
  return true;
}

auto isParam(Texp t) -> bool {
  return t.size() == 1 // TODO param namespace?
      && is(Type::Type, t[0]);
}

auto isStrGet(Texp t) -> bool {
  return t.value == "str-get" && t.size() == 1 && is(Type::IntLiteral, t[0]);
}
auto isMathBinop(Texp t) -> bool {
  return is(Type::Add, t);
}
auto isAdd(Texp t) -> bool {
  return binop("+", t);
}
auto isIcmp(Texp t) -> bool {
  return is(Type::LT, t)
      || is(Type::LE, t)
      || is(Type::GT, t)
      || is(Type::GE, t)
      || is(Type::EQ, t)
      || is(Type::NE, t)
      ;
}
auto isLT(Texp t) -> bool {
  return binop("<", t);
}
auto isLE(Texp t) -> bool {
  return binop("<=", t);
}
auto isGT(Texp t) -> bool {
  return binop(">", t);
}
auto isGE(Texp t) -> bool {
  return binop(">=", t);
}
auto isEQ(Texp t) -> bool {
  return binop("==", t);
}
auto isNE(Texp t) -> bool {
  return binop("!=", t);
}

auto isArgs(Texp t) -> bool {
  if (t.value != "args") return false;

  for (int i = 0; i < t.size(); ++i) {
    if (not is(Type::Expr, t[i])) return false;
  }
  return true;
}

bool binop(std::string op, Texp& t) {
  using namespace Typing;
  return t.value == op
      && t.size() == 3
      && is(Type::Type, t[0])
      && is(Type::Expr, t[1])
      && is(Type::Expr, t[2]);
}

bool Typing::is(Type type, Texp t) {
  switch(type) {
  case Type::Program:       return isProgram(t);
  case Type::TopLevel:      return isTopLevel(t);
  case Type::StrTable:      return isStrTable(t);
  case Type::StrTableEntry: return isStrTableEntry(t);
  case Type::Struct:        return isStruct(t);
  case Type::Field:         return isField(t);
  case Type::Def:           return isDef(t);
  case Type::Decl:          return isDecl(t);
  case Type::Stmt:          return isStmt(t);
  case Type::If:            return isIf(t);
  case Type::Store:         return isStore(t);
  case Type::Auto:          return isAuto(t);
  case Type::Do:            return isDo(t);
  case Type::Return:        return isReturn(t);
  case Type::ReturnExpr:    return isReturnExpr(t);
  case Type::ReturnVoid:    return isReturnVoid(t);
  case Type::Let:           return isLet(t);
  case Type::Value:         return isValue(t);
  case Type::StrGet:        return isStrGet(t);
  case Type::Literal:       return isLiteral(t);
  case Type::IntLiteral:    return isIntLiteral(t);
  case Type::BoolLiteral:   return isBoolLiteral(t);
  case Type::String:        return isString(t);
  case Type::Name:          return isName(t);
  case Type::Types:         return isTypes(t);
  case Type::Type:          return isType(t);
  case Type::Params:        return isParams(t);
  case Type::Param:         return isParam(t);
  case Type::Expr:          return isExpr(t);
  case Type::MathBinop:     return isMathBinop(t);
  case Type::Icmp:          return isIcmp(t);
  case Type::LT:            return isLT(t);
  case Type::LE:            return isLE(t);
  case Type::GT:            return isGT(t);
  case Type::GE:            return isGE(t);
  case Type::EQ:            return isEQ(t);
  case Type::NE:            return isNE(t);
  case Type::Load:          return isLoad(t);
  case Type::Index:         return isIndex(t);
  case Type::Cast:          return isCast(t);
  case Type::Add:           return isAdd(t);
  case Type::Call:          return isCall(t);
  case Type::CallBasic:     return isCallBasic(t);
  case Type::CallVargs:     return isCallVargs(t);
  case Type::CallTail:      return isCallTail(t);
  case Type::Args:          return isArgs(t);

  default: exit(1);
  }
}
