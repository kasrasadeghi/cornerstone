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

    CHECK(choice_index != std::string::npos, s + " is not a choice of " + std::string(choice_type_name));
    CHECK(rest.substr(0, 9) == "/choice->", std::string(rest.substr(7)) + " doesn't have '/choice->' after " + std::string(choice_type_name));
    
    // get the type immediately proceeding the choice
    rest = rest.substr(9);
    std::string type_name = rest.substr(0, rest.find('/'));
    return Typing::parseType(type_name);
  }

using namespace Typing;

/**
 * Calculates the length of the given string, counting escaped characters only once.
 * The string must be well formed:
 *  - Doesn't end in \
 *  - backslashes escaped
 *
 * Escaped Characters: \xx
 *  - x stands for any capital hexadecimal digit.
 */
size_t atomStrLen(const char* s) {
  size_t len = 0;
  for (;*s; s++, len++) {
    if (*s == '\\') {
      int counter = 0;
      s++; counter += isxdigit(*s) != 0;
      s++; counter += isxdigit(*s) != 0;
      if (counter != 2) {
        fprintf(stderr, "backbone: Backslash not followed by two hexadecimal digits.");
        exit(EXIT_FAILURE);
      }
    }
  }
  return len;
}

struct LLVMGenerator {
  Texp root;
  Texp proof;
  LLVMGenerator(Texp t, Texp p): root(t), proof(p) {}
  void Program() 
    {
      print("; ModuleID = ", root.value,
            "\ntarget datalayout = \"e-m:e-i64:64-f80:128-n8:16:32:64-S128\""
            "\ntarget triple = \"x86_64-unknown-linux-gnu\"\n\n");
      print("; ", root, "\n");

      
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
      case Type::Decl:      Decl(texp, proof); break;
      case Type::Def:       Def(texp, proof); break;
      case Type::StrTable:  StrTable(texp, proof); break;
      default: CHECK(false, std::string(getName(t)) + " is unhandled in TopLevel()'s type switch");
      }
      print("\n");
    }
  
  void StrTable(Texp texp, Texp proof)
    {
      for (int i = 0; i < texp.size(); ++i)
        {
          std::string entry = texp[i][0].value;
          // NOTE: we use atomStrLen(...) - 2 because atomStrLen counts the quotes
          print("@str.", i, " = private unnamed_addr constant [", atomStrLen(entry.c_str()) - 2, " x i8] c", entry, ", align 1\n");
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
      /// (types type*)
      print("(");
      int i = 0;
      for (Texp child : texp) 
        {
          print(child.value);
          if (++i != texp.size()) print(", ");
        }
      print(")");
    }
  
  void Def(Texp texp, Texp proof)
    {
      /// (def name params type do)
      print("define ", texp[2].value, " ", texp[0].value, "(");
      Params(texp[1], proof[1]);
      
      print(") {\nentry:\n");
      Do(texp[3], proof[3]);
      print("}\n");
    }
  
  void Params(Texp texp, Texp proof)
    {
      int i = 0;
      for (Texp param : texp)
        {
          //FIXME: use Name and Type?
          print(param[0].value, " ", param.value);
          if (++i != texp.size()) print(", ");
        }
    }
  
  void Do(Texp texp, Texp proof)
    {
      int i = 0;
      for (Texp child : texp)
        {
          Stmt(child, proof[i++]);
        }
    }

  void Stmt(Texp texp, Texp proof)
    {
      print("  ");
      switch(auto t = type(proof, Type::Stmt); t) {
      case Type::Let:       Let(texp, proof); break;
      case Type::Return:    Return(texp, proof); break;
      case Type::Auto:      Auto(texp, proof); break;
      case Type::Store:     Store(texp, proof); break;
      // case Type::If:        If(texp, proof); break;

      // FIXME: we need to pre-verify that all calls that are statements return void
      case Type::Call:      Call(texp, proof); break;
      default: CHECK(false, std::string(getName(t)) + " is unhandled in Stmt()'s type switch");
      }
      print("\n");
    }
  
  void Auto(Texp texp, Texp proof)
    {
      //FIXME: consider adding alloca and using let
      print(texp[0].value, " = alloca ", texp[1].value);
    }
  
  void Store(Texp texp, Texp proof)
    {
      print("store ", texp[1].value, " ");
      Value(texp[0], proof[0]);
      print(", ", texp[1].value, "* ", texp[2].value);
    }
  
  void Let(Texp texp, Texp proof)
    {
      print(texp[0].value, " = ");
      Expr(texp[1], proof[1]);
    }
  
  void Return(Texp texp, Texp proof)
    {
      switch(auto t = type(proof, Type::Return); t) {
      case Type::ReturnExpr: ReturnExpr(texp, proof); break;
      // case Type::ReturnVoid:       ReturnVoid(texp, proof); return;
      default: CHECK(false, std::string(getName(t)) + " is unhandled in Return()'s type switch");
      }
    }
  
  void ReturnExpr(Texp texp, Texp proof)
    {
      print("ret ");
      Type(texp[1], proof[1]);
      print(" ");
      Value(texp[0], proof[0]);
    }
  
  void Value(Texp texp, Texp proof)
    {
      switch(auto t = type(proof, Type::Value); t) {
      case Type::Name:    Name(texp, proof); return;
      case Type::Literal: Literal(texp, proof); return;
      case Type::StrGet:  StrGet(texp, proof); return;
      default: CHECK(false, std::string(getName(t)) + " is unhandled in Value()'s type switch");
      }
    }
  
  void Literal(Texp texp, Texp proof)
    {
      // Note: could switch between Bool- and IntLiterals, but there is no
      // difference in the procedure
      print(texp.value);
    }
  
  void StrGet(Texp texp, Texp proof)
    {
      // (str-get index)

      bool found = false;
      
      for (int i = 0; i < this->root.size(); ++i)
        {
          auto& table = this->root[i];
          auto& table_proof = this->proof[i];
          if (type(table_proof, Type::TopLevel) == Type::StrTable)
            {
              found = true;
              size_t index = std::strtoul(texp[0].value.c_str(), nullptr, 10);
              size_t strlen = atomStrLen(table[index][0].value.c_str()) - 2;

              print("getelementptr inbounds ([", strlen, " x i8], [", strlen, " x i8]* @str.", index, ", i64 0, i64 0)");
            }
        }
      
      CHECK(found, "could not find string table in root");
      
    }
  
  void Type(Texp texp, Texp proof)
    {
      print(texp.value);
    }

  void Name(Texp texp, Texp proof)
    {
      print(texp.value);
    }
  
  void Expr(Texp texp, Texp proof)
    {
      switch(auto t = type(proof, Type::Expr); t) {
      case Type::Call: Call(texp, proof); return;
      case Type::Load: Load(texp, proof); return;
      default: CHECK(false, std::string(getName(t)) + " is unhandled in Expr()'s type switch");
      }
    }
  
  void Load(Texp texp, Texp proof)
    {
      // (load type value)
      print("load ", texp[0].value, ", ", texp[0].value, "* ", texp[1].value);
    }
  
  void Call(Texp texp, Texp proof)
    {
      switch(auto t = type(proof, Type::Call); t) {
      case Type::CallBasic: CallBasic(texp, proof); return;
      case Type::CallVargs: CallVargs(texp, proof); return;
      default: CHECK(false, std::string(getName(t)) + " is unhandled in Call()'s type switch");
      }
    }
  
  void CallBasic(Texp texp, Texp proof)
    {
      // (call name types type args)
      print("call ", texp[2].value, " ");
      Types(texp[1], proof[1]);
      print(" ", texp[0].value);
      Args(texp[3], proof[3], texp[1], proof[1]);
    }

  void CallVargs(Texp texp, Texp proof)
    {
          // (call name types type args)
      print("call ", texp[2].value, " ");

      bool found = false;
      for (int i = 0; i < this->root.size(); ++i)
        {
          auto& decl = this->root[i];
          auto& decl_proof = this->proof[i];
          if (type(decl_proof, Type::TopLevel) == Type::Decl && decl[0].value == texp[0].value)
            {
              // FIXME: check that the declaration is compatible with the types
              // of the arguments and the types listing
              found = true;
              Types(decl[1], decl_proof[1]);
            }
        }
      
      CHECK(found, "could not find declaration matching " + texp[0].value);
      
      print(" ", texp[0].value);

      Args(texp[3], proof[3], texp[1], proof[1]);
    }
  
  void Args(Texp texp, Texp proof, Texp types, Texp types_proof)
    {
      print("(");
      int i = 0;
      for (Texp arg : texp)
        {
          Type(types[i], types_proof[i]);
          print(" ");
          Value(arg, proof[i]);

          i++;
          if (i != texp.size()) print(", ");          
        }
      print(")");
    }
};

void generate(Texp texp, Texp proof) 
  {
    LLVMGenerator l(texp, proof);
    l.Program();
  }
