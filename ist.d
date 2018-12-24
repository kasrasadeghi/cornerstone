import std.string;
import std.algorithm;
import std.stdio;
import std.range;
import std.conv;



void main() {
  auto a = "
    Program,
      TopLevel,
      StrTable,
        StrTableEntry,
      Struct,
        Field,
      Def,
      Decl,
    
    Stmt,
      If,
      Store,
      Auto,
      Do,
      Return,
        ReturnExpr,
        ReturnVoid,
      Let,
    
    Value,
      StrGet,
      Literal,
        IntLiteral,
        BoolLiteral,
      String,
      Name,
    
    Types,
      Type,
    
    Params,
      Param,
    
    Expr,
      MathBinop,
      Icmp,
        LT,
        LE,
        GT,
        GE,
        EQ,
        NE,
      Load,
      Index,
      Cast,
      Add,

    Call,
      CallBasic,
      CallVargs,
      CallTail,
      Args,
  ";

  auto els = a.splitLines.map!strip.filter!(x => !x.empty).map!(x => x.dropBack(1)).array;
  auto maxsize = els.map!(s => s.length).reduce!max + 1;
  // foreach (el; els) {
    // writefln("  case Type::%-"~(maxsize.to!string)~"s result = is%s(t); break;", el~":", el);
    // writefln("  case Type::%-"~(maxsize.to!string)~"s out << \"%s\"; return out;", el~":", el); 
  // }

  // "{".writeln;
  // foreach(el; els) { 
  //   "  \"".write;
  //   el.write;
  //   "\", ".writeln;
  // }
  // "}".writeln;
  els.each!writeln;

  // els.count.to!string.writeln;


}