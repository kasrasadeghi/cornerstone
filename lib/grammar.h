#pragma once
#include "type.h"
#include <string_view>

class Grammar {
public:
static std::string_view getProduction(Typing::Type type);

};
