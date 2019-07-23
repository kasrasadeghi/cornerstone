#include "parser.h"
#include "pass.h"
#include "gen.h"
#include "matcher.h"
#include "print.h"
#include "io.h"

int main(int argc, char* argv[])
  {
    Texp program = (argc == 1) ? parse() : parse_from_file(argv[1]);

    program = passes(program);

    Grammar bb_g (parse_from_file("docs/bb-grammar.texp")[0]);
    Matcher bb_m {bb_g};

    if (auto proof = bb_m.is(program, "Program"))
      {
        generate(bb_g, program, *proof);
        std::cout << "; " << *proof << std::endl;
      }
    else
      {
        std::cout << "grammar error with file: '" << (argc == 1 ? "STDIN" : argv[2]) << "'" << std::endl;
        exit(1);
      }
  }
