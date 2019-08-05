#pragma once

#include "texp.h"
#include "parser.h"
#include "macros.h"
#include "print.h"

#include <fstream>
#include <filesystem>

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
    CHECK(std::filesystem::exists(filename), "'" + std::string(filename) + "' does not exist.");

    // read file
    std::ifstream input_file(filename.data());

    std::stringstream buffer;
    for (std::string line; std::getline(input_file, line); )
      if (not line.starts_with(";"))
        buffer << line;

    // bind lifetime of memory to this scope while the parser constructs the texps
    auto content = buffer.str();

    Parser p(content);
    return std::move(p.file(std::string(filename)));
  }