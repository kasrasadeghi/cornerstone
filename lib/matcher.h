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

Matcher(Grammar g): grammar(g) 
  {
    grammar_functions.insert({"binop", [&](Texp t, Texp rule) -> std::optional<Texp> { std::string_view symbol = rule[0].value; return binop(symbol, t); }});
  }
std::unordered_map<std::string, std::function<std::optional<Texp>(Texp, Texp)>> grammar_functions;

std::optional<Texp> binop(std::string_view op, Texp t);
std::optional<Texp> matchFunction(const Texp& texp, const Texp& rule);

std::optional<Texp> is(const Texp& t, std::string_view type);
std::optional<Texp> match(const Texp& texp, const Texp& rule);
std::optional<Texp> matchKleene(const Texp& texp, const Texp& rule);
std::optional<Texp> matchValue(const Texp& texp, const Texp& rule);
std::optional<Texp> kleene(Texp texp, std::string_view type, int first = 0);
std::optional<Texp> choice(const Texp& texp, std::vector<std::string_view> types);
std::optional<Texp> exact(Texp texp, std::vector<std::string_view> types);
std::optional<Texp> sequence(Texp texp, std::vector<std::string_view> types, int start, int end);
};

// "Decl"       from "TopLevel/choice->Decl/exact", ::TopLevel
// "IntLiteral" from "Expr/choice->Value/choice->Literal/choice->IntLiteral/exact", ::Literal
Grammar::Type parseChoice(const Grammar& g, const Texp& proof, std::string_view from_choice);

