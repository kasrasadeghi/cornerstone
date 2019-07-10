#include "parser.h"
#include "pass.h"
#include "gen.h"
#include "matcher.h"
#include "print.h"

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

Texp parse_from_file(std::string_view filename)
  {
    // read file
    std::ifstream t(filename.data());
    std::stringstream buffer;
    buffer << t.rdbuf();

    // bind lifetime of memory to this scope while the parser constructs the texps
    auto content = buffer.str();

    Parser p(content);
    return std::move(p.file(std::string(filename)));
  }

void stdin_main()
  {
    auto parse_tree = parse();
    Grammar g (parse_from_file("docs/grammar.texp")[0]);
    Matcher m {g};
    if (auto proof = m.is(parse_tree, "Program"))
      {
        generate(g, parse_tree, *proof);
        std::cout << "; " << *proof << std::endl;
      }
    else
      std::cout << "grammar error" << std::endl;
  }

void file_main(int argc, char* argv[])
  {
    Grammar g (parse_from_file("docs/grammar.texp")[0]);
    Matcher m {g};

    // parse files from argv
    for (int i = 1; i < argc; ++i)
      {
        Texp prog = parse_from_file(argv[i]);

        if (auto proof = m.is(prog, "Program"))
          {
            generate(g, prog, *proof);
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
