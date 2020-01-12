#include "parser.h"
#include "pass.h"
#include "gen.h"
#include "matcher.h"
#include "print.h"
#include "io.h"

int main(int argc, char* argv[])
  {
    if (argc != 2) println("usafe: matcher <test-name>");
    Texp texp = parse_from_file("/home/kasra/projects/backbone-test/matcher/" + std::string(argv[1]) + ".texp");
    Grammar grammar { parse_from_file("/home/kasra/projects/backbone-test/matcher/" + std::string(argv[1]) + ".grammar")[0] };
    Matcher matcher { grammar };
    println(texp.tabs());
    println(matcher.is(texp, "Program").tabs());
  }
