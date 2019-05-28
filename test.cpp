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

TEST(texp, to_string)
  {
    Texp t = Parser::parseTexp("(a i32)");
    ASSERT_EQ(t.size(), 1);
    ASSERT_EQ(t.value, "a");
    ASSERT_EQ(t[0].value, "i32");
    std::stringstream out;
    out << t;
    ASSERT_EQ(out.str(), "(a i32)");
  }

TEST(proof, exact_field)
  {
    using namespace Typing;
    Texp t = Parser::parseTexp("(a i32)");
    auto proof = is(Type::Field, t);
    std::cout << *proof << std::endl;
  }

TEST(proof, exact_add)
  {
    using namespace Typing;
    Texp t = Parser::parseTexp("(+ i32 1 2)");
    std::cout << *is(Type::Add, t) << std::endl;
    std::cout << t << std::endl;
  }

TEST(proof, simple_program)
  {
    Texp t = Parser::parseTexp("(STDIN (decl nop types void))");
    auto proof = Typing::is(Typing::Type::Program, t);
    ASSERT_TRUE(proof);
    std::cout << *proof << std::endl;
  }

TEST(generate, type_from_proof)
  {
    using namespace std;
    string s = "Expr/choice->Value/choice->Literal/choice->IntLiteral/exact";
    ASSERT_EQ(41, s.rfind("->"));
    ASSERT_EQ(53, s.rfind("/"));
    ASSERT_EQ("->IntLiteral/exact", s.substr(s.rfind("->")));

    auto start = s.rfind("->") + 2;
    auto len = s.rfind("/") - start;
    auto type =  s.substr(start, len);
    ASSERT_EQ("IntLiteral", type);

    s = "TopLevel/choice->Decl/exact";
    start = s.rfind("->") + 2;
    len = s.rfind("/") - start;
    type =  s.substr(start, len);
    ASSERT_EQ("Decl", type);
  }

TEST(generate, simple_program)
  {
    Texp t = Parser::parseTexp("(STDIN (decl nop types void))");
    auto proof = Typing::is(Typing::Type::Program, t);
    ASSERT_TRUE(proof);
    generate(t, *proof);
  }
