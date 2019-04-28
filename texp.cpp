#include "texp.h"
#include "macros.h"

Texp::Texp(const std::string& value) : Texp(value, {}) {};
Texp::Texp(const std::string& value, const std::initializer_list<Texp>& children) 
    : value(value), _children(children) {}

bool Texp::empty() const 
  { return _children.empty(); }

size_t Texp::size() const
  { return _children.size(); }

void Texp::push(Texp t) 
  { _children.push_back(t); }

Texp& Texp::operator[](int i) 
  { return _children[i]; }

const Texp& Texp::operator[](int i) const
  { return _children[i]; }

std::ostream& operator<<(std::ostream& out, Texp texp) 
  {
    if (texp.empty()) 
      out << texp.value;
    else
      {
        out << "(" << texp.value << " ";
        for (auto iter = texp._children.begin(); iter < texp._children.end(); ++iter) 
          {
            out << *iter;
            if (iter != texp._children.end() - 1) 
              out << " ";
          }
        out << ")";
      }
    return out;
  }

std::string Texp::tabs(int indent)
  {
    std::string acc;
    for (int i = 0; i < indent; ++i)
      acc += "  ";
    
    acc += value + "\n";
    for (auto& c : _children) {
      acc += c.tabs(indent + 1);
    }
    return acc;
  }
