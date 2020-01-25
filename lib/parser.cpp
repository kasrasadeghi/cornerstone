#include "parser.hpp"
#include "reader.hpp"
#include "macros.hpp"

#include <iostream>
#include <string>
#include <vector>
#include <cassert>

using std::string;
using std::vector;

Texp Parser::_char()
  {
    std::string s;
    assert (_r.peek() == '\'');
    s += _r.get();
    while (not (_r.peek() == '\'' && _r.prev() != '\\')) 
      {
        CHECK(_r.hasNext(), "backbone: reached end of file while parsing character");
        s += _r.get();
      }
    assert(_r.peek() == '\'');
    s += _r.get();
    return Texp(s);
  }

Texp Parser::_string()
  {
    std::string s;
    assert (_r.peek() == '\"');
    s += _r.get();
    while (not (_r.peek() == '\"' && _r.prev() != '\\')) 
      {
        CHECK(_r.hasNext(), "reached end of file while parsing string");
        s += _r.get();
      }
    assert(_r.peek() == '\"');
    s += _r.get();
    return Texp(s);
  }

Parser::Parser(std::string_view v): _r(v) {}

void Parser::whitespace() 
  { while (std::isspace(_r.peek())) _r.get(); }

Texp Parser::texp() 
  {
    whitespace();
    // std::cout << _r.pos() << ": " << *_r << " ";
    if (_r.peek() == '(') return list();
    return atom();
  }

Texp Parser::atom() 
  {
    // std::cout << "parsing atom\n";
    assert(_r.peek() != ')'); //TODO assert *_r isn't special
    if (_r.peek() == '\'') return _char();
    if (_r.peek() == '\"') return _string();
    return Texp(word());
  }

Texp Parser::list()
  {
    // std::cout << "parsing list\n";
    assert(_r.get() == '(');
    
    auto curr = Texp(word());
    whitespace();
    while (_r.peek() != ')')
      {
        CHECK(_r.hasNext(), "reached end of file when parsing list:\n  " + curr.paren());
        curr.push(texp());
        whitespace();
      }
    
    assert(_r.get() == ')');

    return curr;
  }

std::string Parser::word()
  {
    std::string s;
    whitespace();
    CHECK(_r.hasNext(), "reached end of file when parsing word");
    while (_r.hasNext() && _r.peek() != '(' && _r.peek() != ')' && not std::isspace(_r.peek())) 
      {
        s += _r.get();
      }
    return s;
  }

Texp Parser::file(std::string filename)
  {
    Texp result(filename);

    whitespace();
    while(_r.hasNext())
      {
        result.push(texp());
        whitespace();
      }

    return result;
  }
