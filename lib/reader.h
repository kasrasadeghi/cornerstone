#pragma once
#include <string>
#include <string_view>
#include <iostream>
#include <assert.h>

class Reader {
public:
  // string_view's iterator is const
  using iterator = std::string_view::iterator;
private:
  std::string_view _content;
  iterator _iter;
public:
  Reader(std::string_view content)
    : _content(content), _iter(_content.begin()) {}

  char get()
    { return *_iter++; }

  char peek() 
    { return *_iter; }
  
  size_t pos() const
    { return _iter - _content.begin(); }

  iterator end() const
    { return _content.end(); }

  iterator curr() const 
    { return _iter; }
  
  char prev() const
    { 
      if (_iter == _content.begin()) return '\0';
      else return *(_iter - 1); 
    }
  
  bool hasNext() const
    { return _iter < _content.end(); }
  
  friend std::ostream& operator<<(std::ostream& out, Reader& r) 
    {
      auto icopy = r._iter;
      while (icopy != r._content.end())
        out << *icopy++;
      return out;
    }
};