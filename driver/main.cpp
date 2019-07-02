#include "parser.h"
#include "pass.h"
#include "gen.h"
#include "matcher.h"

#include <iostream>
#include <string>
#include <sstream>
#include <fstream>

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
    if (auto proof = Typing::is(Type::Program, parse_tree))
      {
        generate(parse_tree, *proof);
        std::cout << "; " << *proof << std::endl;
      }
    else
      std::cout << "grammar error" << std::endl;
  }

void file_main(int argc, char* argv[])
  {
    // parse files from argv
    for (int i = 1; i < argc; ++i)
      {
        // read file
        std::ifstream t{argv[i]};
        std::stringstream buffer;
        buffer << t.rdbuf();
        auto content = buffer.str();

        Parser p(content);
        Texp prog = p.file(argv[i]);

        if (auto proof = Typing::is(Typing::Type::Program, prog))
          {
            generate(prog, *proof);
            std::cout << "; " << *proof << std::endl;
          }
        else
          {
            std::cout << "grammar error with file: '" << argv[i] << "'" << std::endl;
            exit(1);
          }
      }
  }

int main(int argc, char* argv[])
  {
    if (argc == 1)
      stdin_main();
    else
      file_main(argc, argv);
  }
