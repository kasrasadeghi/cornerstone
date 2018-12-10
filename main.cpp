#include <iostream>
#include <string>
#include <vector>
#include <assert.h>

using std::string;
using std::vector;

#define CHECK(cond, msg) \
  do { \
    if (not cond) { \
      std::cerr << "Assertion `" #cond "` failed in " << __FILE__ << ":" << __LINE__ << "\n" \
        << "   " << msg << "\n"; \
      std::exit(0); \
    } \
  } while(0); \

class Texp {
  string _value;
  vector<Texp> _children;

public:
  Texp(const string& value) : Texp(value, {}) {};
  Texp(const string& value, const std::initializer_list<Texp>& children) 
      : _value(value), _children(children) {}

  bool empty() const 
    { return _children.empty(); }

  void push(Texp t) 
    { _children.push_back(t); }

  friend std::ostream& operator<<(std::ostream& out, Texp texp) 
    {
      if (texp.empty()) 
        {
          out << texp._value;
        } 
      else
        {
          out << "(" << texp._value << " ";
          for (auto iter = texp._children.begin(); iter < texp._children.end(); ++iter) 
            {
              auto& child = *iter;
              out << child;
              if (iter != texp._children.end() - 1) {
                out << " ";
              }
            }
          out << ")";
        }
      return out;
    }
};

string collect_stdin() {
  string acc;
  string line;
  while (std::getline(std::cin, line)) acc += line;
  return acc;
}

class Reader {
  string _content;
  string::iterator _iter;
public:
  Reader(const string& content)
    : _content(content), _iter(_content.begin()) {}
  
  string::iterator operator++() 
    { return ++_iter; }

  string::iterator operator++(int) 
    { return _iter++; }

  char operator*() 
    { return *_iter; }

  string::iterator end() 
    { return _content.end(); }

  const string::iterator curr() const 
    { return _iter; }
  
  char prev() const
    { 
      if (_iter == _content.begin()) return '\0';
      else return *(_iter - 1); 
    }
  
  bool hasNext() const 
    { return _iter != _content.end(); }
  
  friend std::ostream& operator<<(std::ostream& out, Reader r) 
    {
      auto icopy = r._iter;
      while (icopy != r._content.end()) {
        out << *icopy++;
      }
      return out;
    }
};

class Parser {
  Reader _r;

  Texp _char()
    {
      std::string s = "";
      assert (*_r == '\'');
      while (not (*_r == '\'' && _r.prev() != '\\')) 
        {
          CHECK(_r.hasNext(), "backbone: reached end of file while parsing character");
          s += *_r++;
        }
      assert(*_r == '\'');
      s += *_r++;
      return Texp(s);
    }

  Texp _string()
    {
      std::string s = "";
      assert (*_r == '\'');
      while (not (*_r == '\"' && _r.prev() != '\\')) 
        {
          CHECK(_r.hasNext(), "reached end of file while parsing string");
          s += *_r++;
        }
      assert(*_r == '\'');
      s += *_r++;
      return Texp(s);
    }

public:
  Parser(string v): _r(v) {}

  void whitespace() 
    { while (std::isspace(*_r)) _r++; }

  Texp texp() 
    {
      whitespace();
      if (*_r == '(') return list();
      return atom();
    }
  
  Texp atom() 
    {
      if (*_r == '\'') return _char();
      if (*_r == '\"') return _string();
      return Texp(word());
    }
  
  Texp list()
    {
      assert(*_r++ == '(');
      
      auto curr = Texp(word()); //TODO
      while (*_r != ')') 
        {
          CHECK(_r.hasNext(), "reached end of file when parsing list");
          curr.push(texp());
          whitespace();
        }
      
      assert(*_r++ == ')');
      return curr;
    }
  
  std::string word()
    {
      whitespace();
      std::string s = "";
      CHECK(_r.hasNext(), "reached end of file when parsing word");
      while (_r.hasNext() && *_r != '(' && *_r != ')' && not std::isspace(*_r)) {
        s += *_r++;
      }
      return s;
    }

  char operator*() 
    { return *_r; }
  
  string::iterator end() 
    { return _r.end(); }

  const string::iterator curr() 
    { return _r.curr(); }  
};

Texp parse() {
  auto content = collect_stdin();
  Texp result("STDIN");

  Parser p(content);
  p.whitespace();
  while(p.curr() != p.end()) {
    result.push(p.texp());
    p.whitespace();
  }

  return result;
}

int main() {
  auto parse_tree = parse();
  std::cout << parse_tree;
}