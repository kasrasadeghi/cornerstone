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
  const Texp& operator[](int i) const;
  
  friend std::ostream& operator<<(std::ostream& out, Texp texp);
  std::string tabs(int indent = 0);

  decltype(_children)::iterator begin() { return _children.begin(); }
  decltype(_children)::iterator end() { return _children.end(); }
  decltype(_children)::const_iterator begin() const { return _children.begin(); }
  decltype(_children)::const_iterator end() const { return _children.end(); }

  decltype(_children)::reference back() { return _children.back(); }
  decltype(_children)::const_reference back() const { return _children.back(); }
};