#include <iostream>
#include <string>

#include "parser.h"
#include "pass.h"
#include "gen.h"
#include "matcher.h"

std::string collect_stdin() 
  {
    std::string acc;
    std::string line;
    while (std::getline(std::cin, line)) acc += line + "\n";
    return acc;
  }

Texp parse() 
  {
    std::string content = collect_stdin();
    Parser p(content);
    return p.file("STDIN");
  }

void stdin_main()
  {
    using namespace Typing;
    auto parse_tree = parse();
    // std::cout << parse_tree << std::endl;
    auto gen_tree = passes(parse_tree);
    // std::cout << gen_tree << std::endl;
    // generate(parse_tree);
    if (auto result = Typing::is(Type::Program, gen_tree))
      std::cout << *result << std::endl;
    else
      std::cout << "grammar error" << std::endl;
  }

int main()
  { 
    stdin_main();
  }
