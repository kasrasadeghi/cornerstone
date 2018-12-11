#pragma once
#include <initializer_list>
#include <string>
#include <vector>
#include "reader.h"

using std::string;

class Texp {
  std::vector<Texp> _children;
public:
  string value;

  Texp(const string& value);
  Texp(const string& value, const std::initializer_list<Texp>& children);
  bool empty() const;
  size_t size() const;
  void push(Texp t);
  Texp& operator[](int i);
  friend std::ostream& operator<<(std::ostream& out, Texp texp);

  using iterator = decltype(_children)::iterator;
  iterator begin() { return _children.begin(); }
  iterator end() { return _children.end(); }
};