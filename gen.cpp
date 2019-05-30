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
Typing::Type type(const Texp& proof, Typing::Type from_choice)
  {
    const auto& s = proof.value;
    
    // get the location of the Type we're choosing from
    std::string_view choice_type_name = Typing::getName(from_choice);
    unsigned long choice_index = s.find(choice_type_name);
    std::string rest = s.substr(choice_index + choice_type_name.size());

    CHECK(choice_index != 0, s + " is not a choice of " + std::string(choice_type_name));
    CHECK(rest.substr(0, 9) == "/choice->", std::string(rest.substr(7)) + " doesn't have '/choice->' after " + std::string(choice_type_name));
    
    // get the type immediately proceeding the choice
    rest = rest.substr(9);
    std::string type_name = rest.substr(0, rest.find('/'));
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

          TopLevel(subtexp, subproof);
        }
    }
  
  void TopLevel(Texp texp, Texp proof)
    {
      switch(auto t = type(proof, Type::TopLevel); t) {
      case Type::Decl: Decl(texp, proof); break;
      case Type::Def: Def(texp, proof); break;
      default: CHECK(false, std::string(getName(t)) + " is unhandled in TopLevel()'s type switch");
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
