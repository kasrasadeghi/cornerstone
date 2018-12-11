#include "texp.h"
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

class LLVMGenerator {
public:
  Texp t;
  LLVMGenerator(Texp t): t(t) {}
  void program() 
    {
      print("; ModuleID = ", t.value, '\n');
      print("target datalayout = \"e-m:e-i64:64-f80:128-n8:16:32:64-S128\""
            "\ntarget triple = \"x86_64-unknown-linux-gnu\"\n");

      for (auto& texp : t) {
      // switch(Type::of(t)) {
      // case Types::
      // }
      }
      
    }
};

void generate(Texp texp) 
  {
    LLVMGenerator l(texp);
    l.program();
  }
