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

std::string Texp::paren() const
  {
    std::string acc;
    if (empty()) 
      acc += value;
    else
      {
        acc +=  "(" + value + " ";
        for (auto iter = _children.begin(); iter < _children.end(); ++iter) 
          {
            acc += iter->paren();
            if (iter != _children.end() - 1) 
              acc += " ";
          }
        acc += ")";
      }
    return acc;
  }

std::string Texp::tabs(int indent) const
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

std::ostream& operator<<(std::ostream& out, Texp texp) 
  {
    return out << texp.paren();
  }