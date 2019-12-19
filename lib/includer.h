#pragma once
#include "texp.h"
#include "grammar.h"
#include "matcher.h"
#include "pass.h"
#include "result.h"

/// region include ///===----------------------------------------------===///

/// a naive includer, like #include in C but without declaration reconciliation.
struct Includer {
Grammar g;
Matcher m;

Includer():
  g(parse_from_file("docs/bb-type-tall-str-include-grammar.texp")[0]), m(g) {}

Texp Program(const Texp& texp)
  {
    Texp program_proof = RESULT_UNWRAP(m.is(texp, "Program"), "given texp is not a bb-type-tall-str-include Program:\n  " + texp.paren());
    Texp this_program {texp.value};

    for (int i = 0; i < texp.size(); ++i)
      this_program.push(TopLevel(texp[i], program_proof[i]));

    return this_program;
  }

Texp TopLevel(const Texp& texp, const Texp& proof)
  {
    Texp result {"*TopLevel"};
    if (parseChoice(g, proof[i], "TopLevel") == g.shouldParseType("Include"))
      for (auto child : Include(texp[i], proof[i]))
        result.push(child);
    else
      result.push(texp[i]);
    return result;
  }

Texp Include(const Texp& texp, const Texp& proof)
  {
    Texp prog_from_file = parse_from_file(texp[0]);
    Texp prog_proof = RESULT_UNWRAP(m.is(texp, "Program"), "given texp is not a bb-type-tall-str-include Program:\n  " + texp.paren());
    Texp result = Program(result, result_proof);
    result.value = "*TopLevel";
    return result;
  }
};
