#include <gtest/gtest.h>

#include "parser.h"
#include "pass.h"
#include "gen.h"
#include "matcher.h"
#include "io.h"
#include "print.h"
#include "normalize.h"

#include <filesystem>

TEST(matcher, return_void_empty)
  {
    Grammar g {parse_from_file("docs/bb-grammar.texp")[0]};
    Matcher m {g};
    ASSERT_TRUE( m.is(Parser::parseTexp("(return-void)"), "Return"));
    ASSERT_FALSE(m.is(Parser::parseTexp("(return-void 5)"), "Return"));
  }

TEST(parser, string_parsing)
  {
    Texp t = Parser::parseTexp(R"( (0 "Hello World!\00") )");
    ASSERT_TRUE(t.size() == 1);
  }

TEST(matcher, str_table)
  {
    Grammar g {parse_from_file("docs/bb-grammar.texp")[0]};
    Matcher m {g};
    Texp t = Parser::parseTexp(R"((0 "Hello World\00"))");
    ASSERT_TRUE(m.is(t, "StrTableEntry"));
  }

TEST(matcher, let_call)
  {
    Grammar g {parse_from_file("docs/bb-grammar.texp")[0]};
    Matcher m {g};
    Texp t = Parser::parseTexp("(let ignored (call puts (types i8*) i32 (args (str-get 0))))");
    ASSERT_TRUE(m.is(t, "Let"));
  }

TEST(matcher, field)
  {
    Grammar g {parse_from_file("docs/bb-grammar.texp")[0]};
    Matcher m {g};
    Texp t = Parser::parseTexp("(a i32)");
    ASSERT_TRUE(m.is(t, "Field"));
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
    Grammar g {parse_from_file("docs/bb-grammar.texp")[0]};
    Matcher m {g};
    Texp t = Parser::parseTexp("(a i32)");
    auto proof = m.is(t, "Field");
    std::cout << *proof << std::endl;
  }

TEST(proof, exact_add)
  {
    Grammar g {parse_from_file("docs/bb-grammar.texp")[0]};
    Matcher m {g};
    Texp t = Parser::parseTexp("(+ i32 1 2)");
    std::cout << *m.is(t, "Add") << std::endl;
    std::cout << t << std::endl;
  }

TEST(proof, typed_int_literal)
  {
    Grammar g {parse_from_file("docs/bb-type-grammar.texp")[0]};
    Matcher m {g};
    Texp t = Parser::parseTexp("(i32 2)");
    std::cout << *m.is(t, "TypedIntLiteral") << std::endl;
    std::cout << t << std::endl;
  }

TEST(type_from_proof, with_parent)
  {
    // get the location of the Type we're choosing from
    std::string s = "Expr/choice->Value/choice->Literal/choice->IntLiteral/exact";
    std::string_view choice_type_name = "Value";

    // chop off the choice
    unsigned long choice_index = s.find(choice_type_name);
    std::string rest = s.substr(choice_index + choice_type_name.size());
    std::cout << rest << std::endl;

    ASSERT_TRUE(choice_index != std::string::npos);
    ASSERT_TRUE(rest.substr(0, 9) == "/choice->");
    
    CHECK(choice_index != 0, s + " is not a choice of " + std::string(choice_type_name));
    CHECK(rest.substr(0, 9) == "/choice->", std::string(rest.substr(7)) + " doesn't have '/choice->' after " + std::string(choice_type_name));
    
    // get the type immediately proceeding the choice
    rest = rest.substr(9);
    std::cout << rest << std::endl;
    std::string type_name = rest.substr(0, rest.find('/'));
    ASSERT_EQ(type_name, "Literal");
    std::cout << type_name << std::endl;
  }

TEST(StackCounter, ctor)
  {
    Grammar g {parse_from_file("docs/bb-grammar.texp")[0]};
    Matcher m {g};
    Texp t = Parser::parseTexp("(def @main (params) i32 (do (let %$0 (+ i32 1 2)) (return 0 i32) ))");
    ASSERT_TRUE(m.is(t, "Def"));
    StackCounter sc{t, *m.is(t, "Def")};
    ASSERT_EQ(sc._count, 1);
  }

TEST(StackCounter, ctor_if_do)
  {
    Grammar g {parse_from_file("docs/bb-grammar.texp")[0]};
    Matcher m {g};
    Texp t = Parser::parseTexp(
      " (def @main (params) i32 (do "
        " (let %$0 (+ i32 1 2)) "
        " (do "
          " (let %$1 (+ i32 1 2)) "
          " (let %$2 (+ i32 1 2)) "
          " (let %$3 (+ i32 1 2)) "
        " ) "
        " (if true (do "
          " (let %$4 (+ i32 1 2)) "
          " (let %$5 (+ i32 1 2)) "
          " (let %$6 (+ i32 1 2)) "
        " )) "
        " (return 0 i32) "
      " )) "
    );
    ASSERT_TRUE(m.is(t, "Def"));
    // print(*m.is(t, "Def"), '\n');
    StackCounter sc{t, *m.is(t, "Def")};
    ASSERT_EQ(sc._count, 7);
  }

TEST(Normalize, simple_if_stmt)
  {
    Texp prog = Parser::parseTexp("(def main params i32 (do (if (< i32 %argc 3) (do (call @puts (types i8*) i32 (args (str-get 0)))))))");
    Normalize n;
    ASSERT_TRUE(n.m.is(prog, "Def"));
    
    auto cond = prog[3][0][0];
    print(cond, '\n');
    
    auto prog_p = (*n.m.is(prog, "Def"));
    auto cond_p = prog_p[3][0][0];
    print(cond_p, '\n');

    StackCounter sc {prog, prog_p};
    n._sc = &sc;

    print(n.ExprToValue(cond, cond_p), '\n');

  }