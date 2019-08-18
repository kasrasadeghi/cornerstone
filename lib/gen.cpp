#include "texp.h"
#include "macros.h"
#include "print.h"
#include "grammar.h"
#include "matcher.h"
#include "llvmtype.h"

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
  Grammar grammar;
  LLVMGenerator(Grammar g, const Texp& t, const Texp& p): grammar(g), root(t), proof(p) {}
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
      UnionMatch(grammar, "TopLevel", texp, proof,
        {
          {"Decl",     [&](const auto& t, const auto& p) { Decl(t, p); }     },
          {"Def",      [&](const auto& t, const auto& p) { Def(t, p); }      },
          {"StrTable", [&](const auto& t, const auto& p) { StrTable(t, p); } },
          {"Struct",   [&](const auto& t, const auto& p) { Struct(t, p); }   },
        });
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
  
  void Struct(Texp texp, Texp proof)
    {
      // (struct %struct.Name (* Field))
      print(texp[0].value, " = type { ");
      for (int i = 1; i < texp.size(); ++i)
        {
          Type(texp[i][0], proof[i][0]);
          // print(texp[i][0].value);
          if (i != texp.size() - 1) print(", ");
        }
      print(" };");
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
          Type(child.value, proof[i].value);
          if (++i != texp.size()) print(", ");
        }
      print(")");
    }
  
  void Def(Texp texp, Texp proof)
    {
      /// (def name params type do)
      print("define ");
      Type(texp[2], proof[2]);
      print(" ", texp[0].value, "(");
      Params(texp[1], proof[1]);
      
      print(") {\nentry:\n");
      size_t if_count = 0;
      Do(texp[3], proof[3], if_count);
      print("}\n");
    }
  
  void Params(Texp texp, Texp proof)
    {
      int i = 0;
      for (Texp param : texp)
        {
          // (name type)
          Type(param[0], proof[i]);
          print(" ");
          Name(param.value, proof.value);
          if (++i != texp.size()) print(", ");
        }
    }
  
  void Do(Texp texp, Texp proof, size_t& if_count)
    {
      int i = 0;
      for (Texp child : texp)
        {
          Stmt(child, proof[i++], if_count);
        }
    }

  void Stmt(Texp texp, Texp proof, size_t& if_count)
    {
      print("  ");
      UnionMatch(grammar, "Stmt", texp, proof,
        {
          {"Let",    [&](const auto& t, const auto& p) { Let(t, p); }    },
          {"Return", [&](const auto& t, const auto& p) { Return(t, p); } },
          {"Auto",   [&](const auto& t, const auto& p) { Auto(t, p); }   },
          {"Store",  [&](const auto& t, const auto& p) { Store(t, p); }  },
          {"If",     [&](const auto& t, const auto& p) { If(t, p, if_count); } },
          {"Call",   [&](const auto& t, const auto& p) { Call(t, p); }   },
        });
      print("\n");
    }
  
  // FIXME: implement if statements without passing around if_count
  void If(Texp texp, Texp proof, size_t& if_count)
    {
      // (if cond do)
      size_t if_num = if_count++;
      print("br i1 ");
      Value(texp[0], proof[0]);
      print(", label %then", if_num, ", label %post", if_num, "\n");

      print("then", if_num, ":\n");
      Do(texp[1], proof[1], if_count);
      print("  br label %post", if_num, "\n");
      print("post", if_num, ":");
    }
  
  void Auto(Texp texp, Texp proof)
    {
      //FIXME: consider adding alloca and using let
      print(texp[0].value, " = alloca ", texp[1].value);
    }
  
  void Store(Texp texp, Texp proof)
    {
      // store value type loc-value
      print("store ");
      Type(texp[1], proof[1]);
      print(" ");
      Value(texp[0], proof[0]);
      print(", ");
      Type(texp[1], proof[1]);
      print("* ", texp[2].value);
    }
  
  void Let(Texp texp, Texp proof)
    {
      print(texp[0].value, " = ");
      Expr(texp[1], proof[1]);
    }
  
  void Return(Texp texp, Texp proof)
    {
      UnionMatch(grammar, "Return", texp, proof,
        {
          {"ReturnExpr", [&](const auto& t, const auto& p) { ReturnExpr(t, p); } },
          {"ReturnVoid", [&](const auto& t, const auto& p) { ReturnVoid(t, p); } },
        });
    }
  
  void ReturnExpr(Texp texp, Texp proof)
    {
      print("ret ");
      Type(texp[1], proof[1]);
      print(" ");
      Value(texp[0], proof[0]);
    }
  
  void ReturnVoid(Texp texp, Texp proof)
    {
      print("ret void");
    }
  
  void Value(Texp texp, Texp proof)
    {
      UnionMatch(grammar, "Value", texp, proof,
        {
          {"Name",    [&](const auto& t, const auto& p) { Name(t, p); } },
          {"Literal", [&](const auto& t, const auto& p) { Literal(t, p); } },
          {"StrGet",  [&](const auto& t, const auto& p) { StrGet(t, p); } },
        });
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
          if (parseChoice(grammar, table_proof, "TopLevel") == grammar.shouldParseType("StrTable"))
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
      using namespace LLVMType;

      if (isPtr(texp.value))
        {
          std::string type = texp.value;
          auto i = type.crbegin();
          for (; *i == '*' && i < type.crend(); ++i) ;
          size_t indirection_count = i - type.crbegin();

          std::string base_type = type.substr(0, type.length() - indirection_count);
          std::string indirection = type.substr(type.length() - indirection_count);
          
          if (isUnsignedInt(base_type))
            print("i", base_type.substr(1));
          else
            print(base_type);
            
          print(indirection);
        }
      else if (isUnsignedInt(texp.value))
        print("i", texp.value.substr(1));
      else
        print(texp.value);
    }

  void Name(Texp texp, Texp proof)
    {
      print(texp.value);
    }
  
  void Expr(Texp texp, Texp proof)
    {
      UnionMatch(grammar, "Expr", texp, proof,
        {
          {"Call",  [&](const auto& t, const auto& p) { Call(t, p); } },
          {"Load",  [&](const auto& t, const auto& p) { Load(t, p); } },
          {"Icmp",  [&](const auto& t, const auto& p) { Icmp(t, p); } },
          {"Cast",  [&](const auto& t, const auto& p) { Cast(t, p); } },
          {"Index", [&](const auto& t, const auto& p) { Index(t, p); } },
          {"MathBinop", [&](const auto& t, const auto& p) { MathBinop(t, p); }},
        });
    }
  
  void MathBinop(Texp texp, Texp proof)
    {
      // op type value value
      std::string_view opcode = ([](const std::string& value) -> std::string_view {
        if (value == "+") return "add";
        if (value == "-") return "sub";
        if (value == "*") return "mul";
        else CHECK(false, "unexpected opcode: '" + value + "'");
      })(texp.value);

      print(opcode, " ");
      Type(texp[0], proof[0]);
      print(" ");
      Value(texp[1], proof[1]);
      print(", ");
      Value(texp[2], proof[2]);
    }

  void Index(Texp texp, Texp proof)
    {
      // (index PtrValue StructName IntValue) 
      // IntValue has to be an IntLiteral for structs
      // it can be another kind of Int typed value otherwise
      CHECK(texp[1].value.starts_with("%struct."), "type does not start with %struct:\n  " + proof.paren());
      print("getelementptr inbounds ", texp[1].value, ", ", texp[1].value, "* ", texp[0].value, ", i32 0, i32 ");
      Value(texp[2], proof[2]);
    }
  
  void Cast(Texp texp, Texp proof)
    {
      // (bitcast/inttoptr/ptrtoint TypeFrom TypeTo Value)
      print(texp.value, " ");
      Type(texp[0], proof[0]);
      print(" ");
      Value(texp[2], proof[2]);
      print(" to ");
      Type(texp[1], proof[1]);
    }
  
  void Icmp(Texp texp, Texp proof)
    {
      // (comp_binop type left right)
      // comp_binop: < <= > >= == !=   ->   LT LE GT GE EQ NE

      print("icmp ");

      auto t = parseChoice(grammar, proof, "Icmp");

      if (t->name == "EQ")
        print("eq");
      else if (t->name == "NE")
        print("ne");
      else 
        {
          if      (texp[0].value[0] == 'u') print("u");
          else if (texp[0].value[0] == 'i') print("s");
          else
            CHECK(false, "unexpected value for type of icmp: '" + texp[0].value + "'")    
            
          if      (t->name == "LT") print("lt");
          else if (t->name == "LE") print("le");
          else if (t->name == "GT") print("gt");
          else if (t->name == "GE") print("ge");
          else
            CHECK(false, "unexpected kind of icmp: '" + texp.value + "'");
        }
      
      print(" ");
      Type(texp[0], proof[0]);
      print(" ");
      Value(texp[1], proof[1]);
      print(", ");
      Value(texp[2], proof[2]);
    }
  
  void Load(Texp texp, Texp proof)
    {
      // (load type value)e
      print("load "); 
      Type(texp[0], proof[0]);
      print(", ");
      Type(texp[0], proof[0]);
      print("* ", texp[1].value);
    }
  
  void Call(Texp texp, Texp proof)
    {
      UnionMatch(grammar, "Call", texp, proof,
        {
          {"CallBasic",  [&](const auto& t, const auto& p) { CallBasic(t, p); } },
          {"CallVargs",  [&](const auto& t, const auto& p) { CallVargs(t, p); } },
          {"CallTail",  [&](const auto& t, const auto& p) { print("tail "); CallBasic(t, p); } },
        });
    }
  
  void CallBasic(Texp texp, Texp proof)
    {
      // (call name types type args)
      print("call ");
      Type(texp[2], proof[2]);
      print(" ");
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
          if (parseChoice(grammar, decl_proof, "TopLevel") == grammar.shouldParseType("Decl") && decl[0].value == texp[0].value)
            {
              // FIXME: check that the declaration is compatible with the types
              // of the arguments and the types listing
              found = true;
              Types(decl[1], decl_proof[1]);

              print(" ", texp[0].value);
              
              CHECK(texp[3].size() == texp[1].size(), "argument values and their type listings don't match in quantity");
              Args(texp[3], proof[3], texp[1], proof[1]);
            }
        }
      
      CHECK(found, "could not find declaration matching " + texp[0].value);
    }
  
  void Args(Texp texp, Texp proof, Texp types, Texp types_proof)
    {
      print("(");
      int i = 0;
      CHECK(texp.size() == types.size(), "wrong argument count for " + texp.paren());
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

void generate(const Grammar& grammar, const Texp& texp, const Texp& proof)
  {
    LLVMGenerator l(grammar, texp, proof);
    l.Program();
  }
