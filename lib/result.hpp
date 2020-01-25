#pragma once

#include "texp.hpp"


#define RESULT_UNWRAP(optional_value, msg) _RESULT_UNWRAP((optional_value), (msg))

#define _RESULT_UNWRAP(result, msg) \
  ([&]() {                                 \
    if (result.value == "error")           \
      {                                    \
        std::cerr << "Result `" #result "` failed in " << __FILE__ << ":" << __LINE__ << "\n" \
          << "   " << msg << std::endl << std::endl; \
        report_error(result);              \
        std::exit(1);                      \
      }                                    \
    else return result[0];                 \
  })()

[[ noreturn ]] inline void report_error(const Texp& result)
  {
    printerrln(result.tabs());
    std::exit(1);
  }
