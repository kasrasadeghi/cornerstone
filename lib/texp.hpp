#pragma once
#include "reader.hpp"
#include <initializer_list>
#include <string>
#include <vector>

class Texp {
  std::vector<Texp> _children {};
public:
  std::string value;

  Texp(const std::string& value);
  Texp(const std::string& value, const std::initializer_list<Texp>& children);
  size_t size() const;
  void push(Texp t);
  Texp& operator[](int i);
  const Texp& operator[](int i) const;
  const Texp& must_find(std::string_view view) const;
  decltype(Texp::_children)::const_iterator find(std::string_view view) const;
    
  friend std::ostream& operator<<(std::ostream& out, Texp texp);
  std::string tabs(int indent = 0) const;
  std::string paren() const;

  decltype(_children)::iterator begin() { return _children.begin(); }
  decltype(_children)::iterator end() { return _children.end(); }
  decltype(_children)::const_iterator begin() const { return _children.begin(); }
  decltype(_children)::const_iterator end() const { return _children.end(); }

  decltype(_children)::reference back() { return _children.back(); }
  decltype(_children)::const_reference back() const { return _children.back(); }
};
