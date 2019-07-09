#include "parser.h"
#include "texp.h"
#include "print.h"
#include "type.h"
#include "matcher.h"

#include <sstream>
#include <fstream>

int main(int argc, char* argv[]) 
  {
    if (argc != 2) 
      {
        print("usage: typer <filename.bb>");
        exit(1);
      }
    // read file
    std::ifstream t{argv[1]};
    std::stringstream buffer;
    buffer << t.rdbuf();
    auto content = buffer.str();

    Parser p(content);
    Texp prog = p.file(argv[1]);

    auto proof = ([&]() {
      auto optional_proof = Typing::is(Typing::Type::Program, prog);
      if (not optional_proof) 
        {
          print("grammar error with ", argv[1]);
          exit(1);
        }
      else
        return *optional_proof;
    })();
    

    for (int i = 0; i < prog.size(); ++i)
      {
        auto& child = prog[i];
        auto& child_proof = proof[i];
        using namespace Typing;
        if (proof_type(child_proof, Type::TopLevel) == Type::Def)
          {
            print(child, '\n');
          }
      }

    // print(prog);
  }