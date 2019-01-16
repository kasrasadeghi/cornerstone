#pragma once
#include "texp.h"
#include "type.h"
#include <functional>
#include <unordered_map>

namespace Typing {

bool is(Type type, const Texp& t, bool trace = true);
}
