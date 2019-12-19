#include "pass.h"
#include "print.h"

#include "includer.h"
#include "normalize.h"
#include "type_expand.h"
#include "str.h"

/// region StackCounter methods ///===-----------------------------------===///

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
      {"Do",  [&](const auto& t, const auto& p) { Do(t, p); }},
      {"If",  [&](const auto& t, const auto& p) { Do(t[1], p[1]); }},
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
    ss << "%$" << _count++;
    return Texp(ss.str());
  }

/// endregion StackCounter methods ///===--------------------------------===///

/// region public pass runner ///===-------------------------------------===///

Texp passes(const Texp& tree) 
  {
    Texp curr = tree;
    {
      Includer i;
      curr = i.Program(curr);
    }
    {
      Str s;
      curr = s.Program(curr);
    }
    {
      Normalize n;
      curr = n.Program(curr);
    }
    {
      TypeInfer t;
      curr = t.Program(curr);
    }
    return curr;
  }

/// endregion public pass runner ///===----------------------------------===///
