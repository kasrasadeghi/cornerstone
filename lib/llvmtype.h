#include <unordered_set>
#include <string_view>

namespace LLVMType {
inline std::unordered_set<std::string_view> sint_primitives { "i1", "i8", "i16", "i32", "i64" };
inline std::unordered_set<std::string_view> uint_primitives { "u8", "u16", "u32", "u64" };
  
inline bool isInt(std::string_view s)
  { return sint_primitives.contains(s) || uint_primitives.contains(s); }

inline bool isSignedInt(std::string_view s)
  { return sint_primitives.contains(s); }

inline bool isUnsignedInt(std::string_view s)
  { return uint_primitives.contains(s); }

inline bool isPtr(std::string_view s)
  { return s.ends_with('*'); }

inline size_t getSize(std::string_view s)
  {
    CHECK(isInt(s), "should only get size of integral types");
    if (s == "i1") return 1;
    if (s == "i8" || s == "u8") return 8;
    if (s == "i16" || s == "u16") return 16;
    if (s == "i32" || s == "u32") return 32;
    if (s == "i64" || s == "u64") return 64;
    CHECK(false, "unreachable");
  }

};