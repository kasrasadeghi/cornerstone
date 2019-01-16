#pragma once
#include <stddef.h>
#include <string_view>
#include "macros.h"

namespace Typing {

enum class Type : size_t
  {
    Program,
      TopLevel,
      StrTable,
        StrTableEntry,
      Struct,
        Field,
      Def,
      Decl,
    
    Stmt,
      If,
      Store,
      Auto,
      Do,
      Return,
        ReturnExpr,
        ReturnVoid,
      Let,
    
    Value,
      StrGet,
      Literal,
        IntLiteral,
        BoolLiteral,
      String,
      Name,
    
    Types,
      Type,
    
    Params,
      Param,
    
    Expr,
      MathBinop,
      Icmp,
        LT,
        LE,
        GT,
        GE,
        EQ,
        NE,
      Load,
      Index,
      Cast,
      Add,

    Call,
      CallBasic,
      CallVargs,
      CallTail,
      Args,
  };

}

namespace Typing {
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

constexpr const std::string_view& getName(Type t);

std::ostream& operator<<(std::ostream& out, Type t);

Type parseType(const std::string_view& s);
}