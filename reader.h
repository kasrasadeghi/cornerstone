#pragma once
#include <string>
#include <iostream>

class Reader {
  std::string _content;
  std::string::iterator _iter;
public:
  Reader(const std::string& content)
    : _content(content), _iter(_content.begin()) {}
  
  std::string::iterator operator++() 
    { return ++_iter; }

  std::string::iterator operator++(int) 
    { return _iter++; }

  char operator*() 
    { return *_iter; }

  std::string::iterator end() 
    { return _content.end(); }

  const std::string::iterator curr() const 
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