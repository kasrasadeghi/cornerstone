#pragma once
#include "texp.h"
#include "grammar.h"

#include <functional>
#include <unordered_map>
#include <optional>
#include <iostream>
#include <sstream>

class Matcher {
public:
Grammar grammar;

Matcher(Grammar g): grammar(g) {}

Texp is         (const Texp& texp, std::string_view type);
Texp match      (const Texp& texp, const Texp& rule);
Texp matchKleene(const Texp& texp, const Texp& rule);
Texp matchValue (const Texp& texp, const Texp& rule);
Texp kleene     (const Texp& texp, std::string_view type, int first = 0);
Texp choice     (const Texp& texp, const Texp& rule);
Texp exact      (const Texp& texp, const Texp& rule);
Texp sequence   (const Texp& texp, const Texp& rule, int start, int end);
};

// "Decl"       from "TopLevel/choice->Decl/exact", ::TopLevel
// "IntLiteral" from "Expr/choice->Value/choice->Literal/choice->IntLiteral/exact", ::Literal
Grammar::Type parseChoice(const Grammar& g, const Texp& proof, std::string_view from_choice);

