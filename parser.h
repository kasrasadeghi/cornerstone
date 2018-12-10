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


class Parser {
  Reader _r;
  Texp _char();
  Texp _string();

public:
  Parser(std::string v);
  void whitespace();
  Texp texp();
  Texp atom();
  Texp list();
  std::string word();
  char operator*();
  string::iterator end();
  const string::iterator curr();
};