#pragma once

#include "grammar.h"
#include "matcher.h"
#include "io.h"
#include "result.h"

#include <unordered_map>
#include <unordered_set>
#include <string>
#include <vector>

// TODO find somewhere better to put this / generalize this
struct StrEnv {
std::vector<std::string> str_table;
};

struct Str {
Grammar g;
Matcher m;
StrEnv env;

Str(): g(parse_from_file("docs/bb-type-tall-str-grammar.texp")[0]), m(g) {}

Texp Program(const Texp& texp)
  {
    Texp proof = RESULT_UNWRAP(m.is(texp, "Program"), "input is not a bb-type Program");

    Texp this_program {texp.value};

    for (int i = 0; i < texp.size(); ++i)
      this_program.push(TopLevel(texp[i], proof[i]));

    auto str_table = Texp{"str-table"};
    size_t index = 0;
    for (auto&& entry : env.str_table) 
      str_table.push({std::to_string(index++), {entry}});
    this_program.push(str_table);

    return this_program;
  }

Texp TopLevel(const Texp& texp, const Texp& proof)
  {
    if (parseChoice(g, proof, "TopLevel") == g.shouldParseType("Def"))
      return Def(texp, proof);
    else
      return texp;
  }

Texp Def(const Texp& texp, const Texp& proof)
  {
    // def name params type do
    return {"def", {texp[0], texp[1], texp[2], Do(texp[3], proof[3])} };
  }

Texp Do(const Texp& texp, const Texp& proof)
  {
    Texp this_do {"do"};
    for (int i = 0; i < texp.size(); ++i)
      this_do.push(Stmt(texp[i], proof[i]));
    return this_do;
  }

Texp Stmt(const Texp& texp, const Texp& proof)
  {
    auto isName = [this](const Texp& p) {
      return parseChoice(g, p, "Value") == g.shouldParseType("Name");
    };

    Texp this_stmt {""};
    UnionMatch(g, "Stmt", texp, proof, {
      {"Auto",   [&](const Texp& t, const Texp& p) {
        this_stmt = t;
      }},
      {"Do",     [&](const Texp& t, const Texp& p) {
        this_stmt = Do(t, p);
      }},
      {"Return", [&](const Texp& t, const Texp& p) {
        // TODO check type(expr) == return type of this function
        UnionMatch(g, "Return", texp, proof, {
          {"ReturnVoid", [&](const Texp& t, const Texp& p) {
            this_stmt = t;
          }},
          {"ReturnExpr", [&](const Texp& t, const Texp& p) {
            // return value -> return value
            this_stmt = {t.value, {Expr(t[0], p[0])} };
          }},
        });
      }},
      {"If",     [&](const Texp& t, const Texp& p) {
        // if expr do
        this_stmt = {t.value, {Expr(t[0], p[0]), Do(t[1], p[1])}};
      }},
      {"Store",  [&](const Texp& t, const Texp& p) {
        // store newExpr locExpr
        this_stmt = {t.value, {Expr(t[0], p[0]), Expr(t[1], p[1])} };
      }},
      {"Call",   [&](const Texp& t, const Texp& p) {
        this_stmt = Call(t, p);
      }},
      {"Let",    [&](const Texp& t, const Texp& p) {
        this_stmt = {t.value, {t[0], Expr(t[1], p[1])}};
      }},
    });
    return this_stmt;
  }

Texp Expr(const Texp& texp, const Texp& proof)
  {
    auto isName = [this](const Texp& p) {
      return parseChoice(g, p, "Value") == g.shouldParseType("Name");
    };

    Texp this_expr {""};
    UnionMatch(g, "Expr", texp, proof, {
      {"Call",      [&](const Texp& t, const Texp& p) {
        this_expr = Call(t, p);
      }},
      {"MathBinop", [&](const Texp& t, const Texp& p) {
        // + expr expr
        this_expr = {t.value, {Expr(t[0], p[0]), Expr(t[1], p[1])} };
      }},
      {"Icmp",      [&](const Texp& t, const Texp& p) {
        // > expr expr
        this_expr = {t.value, {Expr(t[0], p[0]), Expr(t[1], p[1])} };
      }},
      {"Load",      [&](const Texp& t, const Texp& p) {
        // load expr
        this_expr = {t.value, {Expr(t[0], p[0])} };
      }},
      {"Index",     [&](const Texp& t, const Texp& p) {
        // index loc-expr i-expr
        this_expr = {t.value, {Expr(t[0], p[0]), Expr(t[1], p[1])} };
      }},
      {"Cast",      [&](const Texp& t, const Texp& p) {
        // cast to-type expr
        this_expr = {t.value, {t[0], Expr(t[1], p[1])} };
      }},
      {"Value",     [&](const Texp& t, const Texp& p) {
        this_expr = Value(t, p);
      }},
    });
    return this_expr;
  }

/// call name args
Texp Call(const Texp& texp, const Texp& proof)
  {
    Texp this_args {"args"};
    const Texp& args = texp[1];
    const Texp& args_proof = proof[1];
    for (int i = 0; i < args.size(); ++i)
      this_args.push(Expr(args[i], args_proof[i]));

    return {texp.value, {texp[0], this_args}};
  }

Texp Value(const Texp& texp, const Texp& proof)
  {
    if (parseChoice(g, proof, "Value") == g.shouldParseType("String"))
      {
        auto len = std::to_string(env.str_table.size());
        env.str_table.push_back(texp.value);
        return {"str-get", {len}};
      }
    else
      return texp;
  }
};
