#include <iostream>
#include <string>

#include "parser.h"
#include "pass.h"

std::string collect_stdin() {
  std::string acc;
  std::string line;
  while (std::getline(std::cin, line)) acc += line;
  return acc;
}

Texp parse() {
  auto content = collect_stdin();
  Texp result("STDIN");

  Parser p(content);
  p.whitespace();
  while(p.curr() != p.end()) {
    result.push(p.texp());
    p.whitespace();
  }

  return result;
}

int main() {
  auto parse_tree = parse();
  std::cout << parse_tree;
  auto gen_tree = passes(parse_tree);
  std::cout << gen_tree;
}