#include "pass.h"

// region StackCounter methods

void StackCounter::Let(const Texp& let, const Texp& proof)
  {
    auto name = let[0].value;
    CHECK(name.length() > 1, "name in let should not be empty\n  " + let.paren());
    if (not name.starts_with("%$")) return;

    _count++;
    
    // Bookkeeping for checking that the digits are actually ascending.
    auto str_to_nat = [](std::string_view s) {
      size_t value = 0;
      size_t base = 1;
      const char* curr = s.data();
      while (*curr)
        {
          size_t digit_value = *curr - '0';
          CHECK(digit_value >= 0 && digit_value < 9, *curr + std::string(" is not a digit"));
          value += digit_value * base;
          base *= 10;
          ++curr;
        }
      return value;
    };

    size_t local_count = str_to_nat({name.data() + 2, name.length() - 2});
    _sum += local_count;
  }

void StackCounter::Stmt(const Texp& stmt, const Texp& proof)
  {
    UnionMatch(bb_tall_g, "Stmt", stmt, proof, {
      {"Let", [&](const auto& t, const auto& p) { Let(t, p); }},

    }, /*exhaustive=*/ false);
  }

void StackCounter::Do(const Texp& texp, const Texp& proof)
  {
    for (int i = 0; i < texp.size(); ++i)
      {
        Stmt(texp[i], proof[i]);
      }
  }

StackCounter::StackCounter(const Texp& def, const Texp& proof)
  {
    Do(def[3], proof[3]);
  }

Texp StackCounter::newLocal()
  {
    std::stringstream ss;
    ss << _count++;
    return Texp(ss.str());
  }

// endregion StackCounter methods

// Texp normBlock(const Texp& block)

Texp normDef(const Texp& def, const Texp& proof)
  {
    StackCounter sc(def, proof);
    // normBlock()
  }

Texp normalize(const Texp& texp) 
  {
    Grammar bb_tall_g (parse_from_file("docs/bb-tall-grammar.texp")[0]);
    Matcher bb_tall_m {bb_tall_g};
    Texp bb_tall_proof = CHECK_UNWRAP(bb_tall_m.is(texp, "Program"), "given texp is not a bb-tall Program");

    for (int i = 0; i < texp.size(); ++i)
      if ("Def" == proof_type(bb_tall_g, bb_tall_proof[i], "TopLevel")->name)
        {
          normDef(texp[i], bb_tall_proof[i]);
        }
    
    return texp;
  }

Texp passes(const Texp& tree) 
  {
    return normalize(tree);
  }
