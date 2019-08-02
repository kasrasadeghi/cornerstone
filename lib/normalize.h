#pragma once
#include "texp.h"
#include "grammar.h"
#include "matcher.h"
#include "pass.h"

/// region normalize ///===----------------------------------------------===///

struct Normalize {
Grammar g;
Matcher m;
StackCounter* _sc;

Normalize():
  g(parse_from_file("docs/bb-type-tall-grammar.texp")[0]), m(g), _sc(nullptr) {}

Texp Program(const Texp& texp)
  {
    Texp bb_tall_proof = CHECK_UNWRAP(m.is(texp, "Program"), "given texp is not a bb-type-tall Program:\n  " + texp.paren());
    Texp this_program {texp.value};

    for (int i = 0; i < texp.size(); ++i)
      {
        if (parseChoice(g, bb_tall_proof[i], "TopLevel") == g.shouldParseType("Def"))
          this_program.push(Def(texp[i], bb_tall_proof[i]));
        else
          this_program.push(texp[i]);
      }
    
    return this_program;
  }

Texp Def(const Texp& texp, const Texp& proof)
  {
    // def name params type do
    Texp this_def ("def", {texp[0], texp[1], texp[2]});
    StackCounter sc{texp, proof};
    _sc = &sc;
    this_def.push(Do(texp[3], proof[3]));
    _sc = nullptr;
    return this_def;
  }

Texp Do(const Texp& texp, const Texp& proof)
  {
    Texp this_do {"do"};
    for (int i = 0; i < texp.size(); ++i)
      for (Texp stmt : Stmt(texp[i], proof[i]))
        this_do.push(stmt);
    return this_do;
  }

Texp Stmt(const Texp& texp, const Texp& proof)
  {
    Texp block {"*stmt"};

    // - extracts expression to let statement(-s, because of recursion) that are added to block
    // - adds the values created by those let statements to the destination
    auto extract_expr = [&](const Texp& t, const Texp& p, Texp& destination) {
      Texp wrapper = ExprToValue(t, p);
      assert(wrapper.value == "value-*stmt");
      destination.push(wrapper[0]);
      for (int i = 1; i < wrapper.size(); ++i)
        block.push(wrapper[i]);
    };

    UnionMatch(g, "Stmt", texp, proof, {
      {"Auto",   [&](const Texp& t, const Texp& p) { block.push(t); }},
      {"Do",     [&](const Texp& t, const Texp& p) { block.push(Do(t, p)); }},
      {"Let",    [&](const Texp& t, const Texp& p) {
        // let name expr
        auto wrapper = Let(t, p);
        assert(wrapper.value == "*stmt");
        for (Texp stmt : wrapper)
          block.push(stmt);
      }},
      {"Return", [&](const Texp& t, const Texp& p) { 
        // return expr->value
        // - if not ReturnVoid
        if (parseChoice(g, proof, "Return") == g.shouldParseType("ReturnExpr"))
          {
            Texp this_return {"return"};
            extract_expr(t[0], p[0], this_return);
            block.push(this_return);
          }
        else 
          block.push(t);
      }},
      {"Call",   [&](const Texp& t, const Texp& p) { 
        // call name (args (* expr->value))
        Texp new_args ("args");
        const auto& args = t[1];
        const auto& args_proof = p[1];
        for (int i = 0; i < args.size(); ++i)
          extract_expr(args[i], args_proof[i], new_args);
        
        Texp this_call(t.value, {t[0], new_args});
        block.push(this_call);
      }},
      {"If",     [&](const Texp& t, const Texp& p) {
        // if expr->value do
        Texp this_if {"if"};
        extract_expr(t[0], p[0], this_if);
        this_if.push(Do(t[1], p[1]));
        block.push(this_if);
      }},
      {"Store",  [&](const Texp& t, const Texp& p) { 
        // store expr->value expr->value
        Texp this_store {"store"};
        extract_expr(t[0], p[0], this_store);
        extract_expr(t[1], p[1], this_store);
        block.push(this_store);
      }},
    });
    return block;
  }

/// Let is the only place Expr is not extracted, so we have a special case for this Stmt
Texp Let(const Texp& texp, const Texp& proof)
  {
    Texp block {"*stmt"};
    Texp this_let (texp.value, {texp[0]});

    // - extracts expression to let statement(-s, because of recursion) that are added to block
    // - adds the values created by those let statements to the destination
    auto extract_expr = [&](const Texp& t, const Texp& p, Texp& destination) {
      Texp wrapper = ExprToValue(t, p);
      assert(wrapper.value == "value-*stmt");
      destination.push(wrapper[0]);
      for (int i = 1; i < wrapper.size(); ++i)
        block.push(wrapper[i]);
    };

    UnionMatch(g, "Expr", texp[1], proof[1], {
      {"Call",      [&](const Texp& t, const Texp& p) {
        // call name types type (args (* expr->value))
        Texp this_args ("args");
        const auto& args = t[1];
        const auto& args_proof = p[1];
        for (int i = 0; i < args.size(); ++i)
          extract_expr(args[i], args_proof[i], this_args);
        
        Texp this_call(t.value, {t[0], this_args});
        this_let.push(this_call);
      }},
      {"MathBinop", [&](const Texp& t, const Texp& p) { 
        // + expr->value expr->value
        Texp this_binop (t.value);
        extract_expr(t[0], p[0], this_binop);
        extract_expr(t[1], p[1], this_binop);
        this_let.push(this_binop);
      }},
      {"Icmp",      [&](const Texp& t, const Texp& p) { 
        // < expr->value expr->value
        Texp this_icmp (t.value);
        extract_expr(t[0], p[0], this_icmp);
        extract_expr(t[1], p[1], this_icmp);
        this_let.push(this_icmp);
      }},
      {"Load",      [&](const Texp& t, const Texp& p) { 
        // load expr->value
        Texp this_load (t.value);
        extract_expr(t[0], p[0], this_load);
        this_let.push(this_load);
      }},
      {"Index",     [&](const Texp& t, const Texp& p) { 
        // index expr->value expr->value
        Texp this_index (t.value);
        extract_expr(t[0], p[0], this_index);
        extract_expr(t[1], p[1], this_index);
        this_let.push(this_index);
      }},
      {"Cast",      [&](const Texp& t, const Texp& p) { 
        // cast to-type expr->value
        Texp this_cast (t.value, {t[0]});
        extract_expr(t[1], p[1], this_cast);
        this_let.push(this_cast);
      }},
      {"Value",     [&](const Texp& t, const Texp& p) { 
        print("why are you letting a value? huh?\n");
        exit(1);
      }},
    });

    block.push(this_let);

    return block;
  }

Texp ExprToValue(const Texp& texp, const Texp& proof)
  {

    auto isTall = [&](const auto& proof) -> bool {
      return parseChoice(g, proof, "Expr") != g.shouldParseType("Value");
    };

    Texp wrapper {"value-*stmt"};
    if (isTall(proof))
      {
        Texp local_name = _sc->newLocal();
        wrapper.push(local_name);

        Texp let ("let", {local_name, texp} );
        Texp let_proof = CHECK_UNWRAP(m.is(let, "Let"), "failed to generate let for " + texp.paren());
        for (Texp stmt : Let(let, let_proof))
          wrapper.push(stmt);
        
        return wrapper;
      }
    else
      {
        wrapper.push(texp);
        return wrapper;
      }
  }
};

/// endregion normalize ///===-------------------------------------------===///
