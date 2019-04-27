#include <gtest/gtest.h>

#include "parser.h"
#include "pass.h"
#include "gen.h"
#include "matcher.h"

TEST(matcher, return_void_empty)
  {
    using namespace Typing;
    ASSERT_TRUE(is(Type::Return, Parser::parseTexp("(return-void)")));
    ASSERT_FALSE(is(Type::Return, Parser::parseTexp("(return-void 5)")));
  }

TEST(parser, string_parsing)
  {
    Texp t = Parser::parseTexp(R"( (0 "Hello World!\00") )");
    ASSERT_TRUE(Typing::is(Typing::Type::StrTableEntry, t));
    ASSERT_TRUE(t.size() == 1);
  }

TEST(matcher, str_table)
  {
    Texp t = Parser::parseTexp(R"((0 "Hello World\00"))");
    ASSERT_TRUE(Typing::is(Typing::Type::StrTableEntry, t));
  }

TEST(matcher, let_call)
  {
    Texp t = Parser::parseTexp("(let ignored (call puts (types i8*) i32 (args (str-get 0))))");
    ASSERT_TRUE(Typing::is(Typing::Type::Let, t));
  }

TEST(matcher, field)
  {
    Texp t = Parser::parseTexp("(a i32)");
    ASSERT_TRUE(Typing::is(Typing::Type::Field, t));
  }

TEST(proof, field)
  {
    Texp t = Parser::parseTexp("(a i32)");
    auto proof = Typing::is(Typing::Type::Field, t);
    ASSERT_TRUE(proof);

    if (proof)
      std::cout << *proof << std::endl;
  }