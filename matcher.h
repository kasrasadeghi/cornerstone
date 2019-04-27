#pragma once
#include "texp.h"
#include "type.h"
#include <functional>
#include <unordered_map>
#include <iostream>
#include <sstream>

namespace Typing {

// bool is(Type type, const Texp& t, bool trace = true);
std::optional<Texp> is(Type type, const Texp& t);
}

////////// global data storage /////////////////

inline std::stringstream is_buffer;
