#pragma once
#include "texp.h"
#include <functional>
#include <unordered_map>

enum class Types 
  {
    Program,
      StrTable,
        StrTableEntry,
      Struct,
      Def,
      Decl,
    
    
    Stmt,
      If,
      Store,
      Auto,
      Do,
      Return,
      VoidReturn,
      Let,
    
    Value,
      StrGet,
      IntLiteral,
      BoolLiteral,
      StringLiteral,
      Name,
    
    Types,
      Type,
    
    Params,
      Param,
    
    Expr,
      MathBinop,
      Icmp,
      Load,
      Index,
      Cast,
      Add,

    Call,
      CallVargs,
      CallTail,
  };

namespace Type 
  {
    static std::unordered_map<::Types, std::function<bool(Texp)>> is;
    Types of(Texp t) 
      {
        
      }
  }
