#pragma once
#include <iostream>

#define CHECK(cond, msg) \
  do { \
    if (not cond) { \
      std::cerr << "Assertion `" #cond "` failed in " << __FILE__ << ":" << __LINE__ << "\n" \
        << "   " << msg << "\n"; \
      std::exit(0); \
    } \
  } while(0);
