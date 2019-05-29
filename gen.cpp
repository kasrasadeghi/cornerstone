#include "texp.h"
#include "macros.h"
#include "type.h"
#include <stdio.h>
#include <iostream>

template <class Arg>
void print(Arg const& arg) 
  {
    std::cout << arg;
  }

template <class Arg, class... Args>
void print(Arg const& arg, Args const&... args)
  {
    std::cout << arg;
    print(args...);
  }

// "Decl"       from "TopLevel/choice->Decl/exact"
// "IntLiteral" from "Expr/choice->Value/choice->Literal/choice->IntLiteral/exact"
Typing::Type type(const Texp& proof)
  {
    const auto& s = proof.value;
    
    auto start = s.rfind("->") + 2;
    auto len = s.rfind("/") - start;
    std::string_view type_name = s.substr(start, len);

    return Typing::parseType(type_name);
  }

struct LLVMGenerator {
  Texp root;
  Texp proof;
  LLVMGenerator(Texp t, Texp p): root(t), proof(p) {}
  void Program() 
    {
      using namespace Typing;
      print("; ModuleID = ", root.value, '\n');
      print("target datalayout = \"e-m:e-i64:64-f80:128-n8:16:32:64-S128\""
            "\ntarget triple = \"x86_64-unknown-linux-gnu\"\n\n");

      
      CHECK(root.size() == proof.size(), "proof should be the same size as texp");
      for (int i = 0; i < root.size(); ++i) 
        {
          auto subtexp = root[i];
          auto subproof = proof[i];

          switch(auto t = type(subproof); t) {
          case Type::Decl: Decl(subtexp, subproof); break;
          case Type::Def: Def(subtexp, subproof); break;
          default: CHECK(false, std::string(getName(t)) + " is unhandled in program()'s type switch");
          }
        }
    }
  
  void Decl(Texp texp, Texp proof)
    {
      /// (decl name types type)
      print("declare ", texp[2].value, " ", texp[0].value);
      Types(texp[1], proof[1]);
    }
  
  void Types(Texp texp, Texp proof)
    {
      print("(");
      int i = 0;
      for (Texp child : texp) 
        {
          print(child.value);
          if (++i != texp.size()) print(", ");
        }
      print(")\n");
    }
  
  void Def(Texp texp, Texp proof)
    {
      print("define ", texp[2].value, " ", s[0], "(");
      int i = 0;
      for (Texp child : texp)
        {
          print(child[0].value, " %", child.value);
          if (++i != texp.size()) print(", ");
        }
      print(") {\nentry:\n");
      Do(texp[3], proof[3]);
      print("}\n");
    }
  
  void Do(Texp texp, Texp proof)
    {
      for (Texp child : texp)
        {
          Stmt(child);
        }
    }

  void Stmt(Texp texp, Texp proof)
    {
      switch(type(proof)) {
      case Type::Let: Let(texp, proof); return;
      case Type::Return: Let(texp, proof); return;
      case Type::If: Let(texp, proof); return;
      case Type::Call: Let(texp, proof); return;
      case Type::Let: Let(texp, proof); return;
      }
    }
};

void generate(Texp texp, Texp proof) 
  {
    LLVMGenerator l(texp, proof);
    l.Program();
  }
