#include <gtest/gtest.h>

#include "parser.h"
#include "pass.h"
#include "gen.h"
#include "matcher.h"

TEST(matcher, return_void_empty)
  {
    using namespace Typing;
    std::cout << is(Type::Return, Parser::parseTexp("(return-void)")) << std::endl;
    std::cout << is(Type::Return, Parser::parseTexp("(return-void 5)")) << std::endl;
  }

TEST(parser, string_parsing)
  {
    Texp t = Parser::parseTexp(R"( (0 "Hello World!\00") )");
    std::cout << t << std::endl;
    std::cout << Typing::is(Typing::Type::StrTableEntry, t) << std::endl;
    std::cout << t.tabs() << std::endl;
  }

TEST(matcher, str_table)
  {
    Texp t = Parser::parseTexp(R"((0 "Hello World\00"))");
    std::cout << t << std::endl;
    std::cout << Typing::is(Typing::Type::StrTableEntry, t) << std::endl;
    std::cout << t.tabs() << std::endl;
  }

TEST(matcher, let_call_test)
  {
    Texp t = Parser::parseTexp("(let ignored (call puts (types i8*) i32 (args (str-get 0))))");
    std::cout << t << std::endl;
    std::cout << t.tabs() << std::endl;
    std::cout << Typing::is(Typing::Type::Let, t) << std::endl;
  }

TEST(matcher, field_test)
  {
    Texp t = Parser::parseTexp("(a i32)");
    std::cout << t << std::endl;
    std::cout << t.tabs() << std::endl;
    std::cout << Typing::is(Typing::Type::Field, t) << std::endl;
  }