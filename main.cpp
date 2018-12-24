#include <iostream>
#include <string>

#include "parser.h"
#include "pass.h"
#include "gen.h"
#include "grammar.h"

std::string collect_stdin() 
  {
    std::string acc;
    std::string line;
    while (std::getline(std::cin, line)) acc += line;
    return acc;
  }

Texp parse() 
  {
    Parser p(collect_stdin());
    return std::move(p.file("STDIN"));
  }

void stdin_main()
  {
    using namespace Typing;
    auto parse_tree = parse();
    // std::cout << parse_tree << std::endl;
    auto gen_tree = passes(parse_tree);
    // std::cout << gen_tree << std::endl;
    // generate(parse_tree);
    std::cout << is(Type::Program, gen_tree) << std::endl;
  }

void return_void_empty_test()
  {
    using namespace Typing;
    std::cout << is(Type::Return, Parser::parseTexp("(return-void)")) << std::endl;
    std::cout << is(Type::Return, Parser::parseTexp("(return-void 5)")) << std::endl;
  }

void string_parsing_test() 
  {
    Texp t = Parser::parseTexp(R"( (0 "Hello World!\00") )");
    std::cout << t << std::endl;
    std::cout << Typing::is(Typing::Type::StrTableEntry, t) << std::endl;
    std::cout << t.tabs() << std::endl;
  }

void str_table_test()
  {
    Texp t = Parser::parseTexp("(0 \"Hello World\\00\")");
    std::cout << t << std::endl;
    std::cout << Typing::is(Typing::Type::StrTableEntry, t) << std::endl;
    std::cout << t.tabs() << std::endl;
  }

void let_call_test()
  {
    Texp t = Parser::parseTexp("(let ignored (call puts (types i8*) i32 (args (str-get 0))))");
    std::cout << t << std::endl;
    std::cout << t.tabs() << std::endl;
    std::cout << Typing::is(Typing::Type::Let, t) << std::endl;
  }

int main()
  { 
    // stdin_main();
    let_call_test();
  }