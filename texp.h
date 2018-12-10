#pragma once
#include <initializer_list>
#include <string>
#include <vector>
#include "reader.h"

using std::string;

class Texp {
  string _value;
  std::vector<Texp> _children;
public:
  Texp(const string& value);
  Texp(const string& value, const std::initializer_list<Texp>& children);
  bool empty() const;
  void push(Texp t);
  friend std::ostream& operator<<(std::ostream& out, Texp texp);
};