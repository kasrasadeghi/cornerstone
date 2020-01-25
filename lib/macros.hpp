#pragma once

#include <iostream>
#include <functional>

/// CHECK

#define CHECK(cond, msg) _CHECK((cond), (msg))

#define _CHECK(cond, msg) \
  do { \
    if (not cond) { \
      std::cerr << "\nAssertion `" #cond "` failed in " << __FILE__ << ":" << __LINE__ << "\n" \
                << "   " << msg << std::endl << std::endl;                                     \
      assert(0); \
    } \
  } while(0);

#define CHECK_UNWRAP(optional_value, msg) _CHECK_UNWRAP((optional_value), (msg))

#define _CHECK_UNWRAP(optional_value, msg) \
  ([&]() {                                 \
    if (not optional_value)                \
      {                                    \
        std::cerr << "Optional `" #optional_value "` not present in " << __FILE__ << ":" << __LINE__ << "\n" \
          << "   " << msg << std::endl;    \
        std::exit(1);                      \
      }                                    \
    else return *optional_value;           \
  })()

/// DEFER
// from https://oded.blog/2017/10/05/go-defer-in-cpp/

class ScopeGuard {
public:
template<class Callable>
ScopeGuard(Callable &&fn) : fn_(std::forward<Callable>(fn)) {}

ScopeGuard(ScopeGuard &&other) : fn_(std::move(other.fn_)) 
  { other.fn_ = nullptr; }

~ScopeGuard() 
  { /* must not throw */ if (fn_) fn_(); }

ScopeGuard(const ScopeGuard &) = delete;
void operator=(const ScopeGuard &) = delete;

private:
std::function<void()> fn_;
};

#define DEFER_CONCAT_(a, b) a ## b
#define DEFER_CONCAT(a, b) DEFER_CONCAT_(a,b)
#define DEFER(fn) ScopeGuard DEFER_CONCAT(__defer__, __LINE__) = [&] ( ) { fn ; }
