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

struct LLVMGenerator {
  Texp root;
  Texp proof;
  LLVMGenerator(Texp t, Texp p): root(t), proof(p) {}
  void program() 
    {
      print("; ModuleID = ", root.value, '\n');
      print("target datalayout = \"e-m:e-i64:64-f80:128-n8:16:32:64-S128\""
            "\ntarget triple = \"x86_64-unknown-linux-gnu\"\n");

      
      CHECK(root.size() == proof.size(), "proof should be the same size as texp");
      for (int i = 0; i < root.size(); ++i) {
        auto subtexp = root[i];
        auto subproof = proof[i];
      }
    }
};

void generate(Texp texp, Texp proof) 
  {
    LLVMGenerator l(texp, proof);
    l.program();
  }
