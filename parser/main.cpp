#include "parser.hpp"
#include "print.hpp"
#include "io.hpp"

int main(int argc, char* argv[])
  {
    Texp program = (argc == 1) ? parse() : parse_from_file(argv[1]);
    print(program.tabs());
  }
