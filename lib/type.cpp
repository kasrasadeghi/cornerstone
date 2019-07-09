#include "type.h"
#include "macros.h"

std::ostream& Typing::operator<<(std::ostream& out, Typing::Type t) 
  { return out << getName(t); }

Typing::Type Typing::parseType(const std::string_view& s) 
  {
    auto index = std::find(Typing::type_names.begin(), Typing::type_names.end(), s);
    CHECK(index != Typing::type_names.end(), "Type from string: '" + std::string(s) + "' not found");
    return static_cast<Typing::Type>(index - Typing::type_names.begin()); 
  }

// "Decl"       from "TopLevel/choice->Decl/exact", ::TopLevel
// "IntLiteral" from "Expr/choice->Value/choice->Literal/choice->IntLiteral/exact", ::Literal
Typing::Type Typing::proof_type(const Texp& proof, Typing::Type from_choice)
  {
    const auto& s = proof.value;

    // get the location of the Type we're choosing from
    std::string_view choice_type_name = Typing::getName(from_choice);
    unsigned long choice_index = s.find(choice_type_name);
    std::string rest = s.substr(choice_index + choice_type_name.size());

    CHECK(choice_index != std::string::npos, s + " is not a choice of " + std::string(choice_type_name));
    CHECK(rest.substr(0, 9) == "/choice->", std::string(rest.substr(7)) + " doesn't have '/choice->' after " + std::string(choice_type_name));
    
    // get the type immediately proceeding the choice
    rest = rest.substr(9);
    std::string type_name = rest.substr(0, rest.find('/'));
    return Typing::parseType(type_name);
  }