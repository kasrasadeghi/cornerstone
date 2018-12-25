#pragma once
#include "texp.h"
#include <functional>
#include <unordered_map>

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

bool is(Type type, const Texp& t, bool trace = true);
}
