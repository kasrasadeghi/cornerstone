#pragma once

#include "texp.h"
#include "parser.h"

#include <fstream>

inline std::string collect_stdin() 
  {
    std::string acc;
    std::string line;
    while (std::getline(std::cin, line)) acc += line + "\n";
    return acc;
  }

inline Texp parse() 
  {
    std::string content = collect_stdin();
    Parser p(content);
    return p.file("STDIN");
  }

inline Texp parse_from_file(std::string_view filename)
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