#include <gtest/gtest.h>

#include "parser.hpp"
#include "pass.hpp"
#include "gen.hpp"
#include "matcher.hpp"
#include "io.hpp"
#include "print.hpp"
#include "normalize.hpp"
#include "llvmtype.hpp"

#include <filesystem>

TEST(parser, string_parsing)
  {
    Texp t = Parser::parseTexp(R"( (0 "Hello World!\00") )");
    ASSERT_TRUE(t.size() == 1);
  }

TEST(parser, atoms_space)
  {
    Texp t = parse_from_file("../backbone-test/texp-parser/atoms_space.texp");
    println(t.tabs());
    ASSERT_EQ(t.size(), 3);
  }

TEST(parser, atoms_newline)
  {
    Texp t = parse_from_file("../backbone-test/texp-parser/atoms_newline.texp");
    println(t.tabs());
    ASSERT_EQ(t.size(), 3);
  }

TEST(matcher, return_void_empty)
  {
    Grammar g {parse_from_file("docs/bb-grammar.texp")[0]};
    Matcher m {g};
    ASSERT_EQ("success", m.is(Parser::parseTexp("(return-void)"), "Return").value);
    ASSERT_EQ("error"  , m.is(Parser::parseTexp("(return-void 5)"), "Return").value);
  }

TEST(matcher, str_table)
  {
    Grammar g {parse_from_file("docs/bb-grammar.texp")[0]};
    Matcher m {g};
    Texp t = Parser::parseTexp(R"((0 "Hello World\00"))");
    ASSERT_EQ("success", m.is(t, "StrTableEntry").value);
  }

TEST(matcher, let_call)
  {
    Grammar g {parse_from_file("docs/bb-grammar.texp")[0]};
    Matcher m {g};
    Texp t = Parser::parseTexp("(let ignored (call puts (types i8*) i32 (args (str-get 0))))");
    ASSERT_EQ("success", m.is(t, "Let").value);
  }

TEST(matcher, field)
  {
    Grammar g {parse_from_file("docs/bb-grammar.texp")[0]};
    Matcher m {g};
    Texp t = Parser::parseTexp("(a i32)");
    ASSERT_EQ("success", m.is(t, "Field").value);
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
    std::cout << proof << std::endl;
    ASSERT_EQ("success", proof.value);
  }

TEST(proof, exact_add)
  {
    Grammar g {parse_from_file("docs/bb-grammar.texp")[0]};
    Matcher m {g};
    Texp t = Parser::parseTexp("(+ i32 1 2)");
    std::cout << m.is(t, "Add") << std::endl;
    std::cout << t << std::endl;
  }

TEST(proof, typed_int_literal)
  {
    Grammar g {parse_from_file("docs/bb-type-grammar.texp")[0]};
    Matcher m {g};
    Texp t = Parser::parseTexp("(i32 2)");
    std::cout << m.is(t, "TypedIntLiteral") << std::endl;
    std::cout << t << std::endl;
  }

TEST(proof, kleene_structure)
  {
    Grammar g {parse_from_file("docs/bb-grammar.texp")[0]};
    Matcher m {g};

    Texp t = Parser::parseTexp("(struct %struct.MyStruct (a i64) (b i64))");
    print(t, '\n');
    ASSERT_EQ("success", m.is(t, "Struct").value);
    Texp tp = m.is(t, "Struct");
    print(tp, '\n');
    ASSERT_EQ(t.size(), tp[0].size());

    Texp args = Parser::parseTexp("(args 0 0 0)");
    print(args, '\n');

    ASSERT_EQ("success", m.is(args, "Args").value);
    Texp proof = m.is(args, "Args");
    print(proof, '\n');
    ASSERT_EQ(proof[0].size(), args.size());
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
    ASSERT_EQ("success", m.is(t, "Def").value);
    StackCounter sc{t, m.is(t, "Def")[0]};
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
    ASSERT_EQ("success", m.is(t, "Def").value);
    // print(*m.is(t, "Def"), '\n');
    StackCounter sc{t, m.is(t, "Def")[0]};
    ASSERT_EQ(sc._count, 7);
  }

TEST(Normalize, simple_if_stmt)
  {
    Texp prog = Parser::parseTexp("(def main params i32 (do (if (< %argc 3) (do (call @puts (args (str-get 0)))))))");
    Normalize n;
    ASSERT_EQ("success", n.m.is(prog, "Def").value);
    
    auto cond = prog[3][0][0];
    print(cond, '\n');
    
    auto prog_p = RESULT_UNWRAP(n.m.is(prog, "Def"), "should be def");
    auto cond_p = prog_p[3][0][0];
    print(cond_p, '\n');

    StackCounter sc {prog, prog_p};
    n._sc = &sc;

    print(n.ExprToValue(cond, cond_p), '\n');
  }

TEST(gen, indirection_count)  
  {
    using namespace LLVMType;

    std::string type = "u64***";
    auto i = type.crbegin();
    for (; *i == '*' && i < type.crend(); ++i) ;

    size_t indirection_count = i - type.crbegin();

    print(type.substr(0, type.length() - indirection_count), '\n');

    ASSERT_EQ(3, indirection_count);
  }

TEST(string, remove_quotes)
  {
    std::string str = "\"what\"";
    std::string contents = str.substr(1, str.length() - 2);
    ASSERT_EQ(contents, "what");
  }
