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
  std::string tabs(int indent = 0);

  using iterator = decltype(_children)::iterator;
  iterator begin() { return _children.begin(); }
  iterator end() { return _children.end(); }
  decltype(_children)::reference back() { return _children.back(); }
};