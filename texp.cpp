#include "texp.h"
#include "check.h"

Texp::Texp(const string& value) : Texp(value, {}) {};
Texp::Texp(const string& value, const std::initializer_list<Texp>& children) 
    : value(value), _children(children) {}

bool Texp::empty() const 
  { return _children.empty(); }

size_t Texp::size() const
  { return _children.size(); }

void Texp::push(Texp t) 
  { _children.push_back(t); }

Texp& Texp::operator[](int i) 
  { return _children[i]; }

std::ostream& operator<<(std::ostream& out, Texp texp) 
  {
    if (texp.empty()) 
      {
        out << texp.value;
      } 
    else
      {
        out << "(" << texp.value << " ";
        for (auto iter = texp._children.begin(); iter < texp._children.end(); ++iter) 
          {
            auto& child = *iter;
            out << child;
            if (iter != texp._children.end() - 1) 
              {
                out << " ";
              }
          }
        out << ")";
      }
    return out;
  }

