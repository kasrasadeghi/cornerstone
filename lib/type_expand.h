#pragma once

#include "grammar.h"
#include "matcher.h"
#include "io.h"
#include "llvmtype.h"
#include "result.h"

#include <unordered_map>
#include <unordered_set>
#include <string>

// TODO find somewhere better to put this / generalize this
struct TypeExpandEnv {
std::unordered_map<std::string, Texp> globals;
Texp* _current_def_return_type;
std::unordered_map<std::string, std::string> _locals;
Texp* curr_def_name;

public:
void addLocal(const std::string& key, const std::string& value)
  {
    print("; ", key, " -> ", value, '\n');
    _locals.emplace(key, value);
  }

Texp lookup(const std::string& name)
  {
    if (name.starts_with('%') && not name.starts_with("%struct."))
      {
        CHECK(_locals.contains(name), "no local named " + name);
        return Texp{_locals.at(name)};
      }
    else
      {
        CHECK(globals.contains(name), "no global named " + name);
        return globals.at(name);
      }
  }

// gets the type of a Value
Texp typeOf(const Grammar& g, const Texp& texp, const Texp& proof)
  {
    Texp type {""};
    UnionMatch(g, "Value", texp, proof, {
      {"Name",    [&](const Texp& t, const Texp& p) { type = lookup(t.value); }},
      {"StrGet",  [&](const Texp& t, const Texp& p) { type.value = "i8*"; }},
      {"Literal", [&](const Texp& t, const Texp& p) { 
        if (parseChoice(g, p, "Literal") == g.shouldParseType("BoolLiteral"))
          type.value = "i1";
        else if (parseChoice(g, p, "Literal") == g.shouldParseType("IntLiteral"))
          {
            if (parseChoice(g, p, "IntLiteral") == g.shouldParseType("TypedIntLiteral"))
              type.value = t[0].value;
            else
              type.value = "int";
          }
        else
          {
            print("error: literal is neither Int nor Bool\n");
            exit(1);
          }
      }},
    });
    return type;
  }
};

struct TypeInfer {
Grammar g;
Matcher m;
TypeExpandEnv env;

TypeInfer(): g(parse_from_file("docs/bb-type-grammar.texp")[0]), m(g) {}

Texp Program(const Texp& texp)
  {
    Texp proof = RESULT_UNWRAP(m.is(texp, "Program"), "input is not a bb-type Program");

    Texp this_program {texp.value};

    for (int i = 0; i < texp.size(); ++i)
      TopLevelEnv(texp[i], proof[i]);

    for (int i = 0; i < texp.size(); ++i)
      this_program.push(TopLevel(texp[i], proof[i]));
    return this_program;
  }

// builds env.globals using TopLevel definitions
void TopLevelEnv(const Texp& texp, const Texp& proof)
  {
    // TODO store proof in globals
    UnionMatch(g, "TopLevel", texp, proof, {
      //StrTable Struct Def Decl
      {"Struct", [&](const Texp& t, const Texp& p) {
        env.globals.emplace(t[0].value, t);
      }},
      {"Def",    [&](const Texp& t, const Texp& p) {
        env.globals.emplace(t[0].value, t);
      }},
      {"Decl",   [&](const Texp& t, const Texp& p) {
        env.globals.emplace(t[0].value, t);
      }},
      {"StrTable", [&](const Texp& t, const Texp& p) { /* do nothing */ }},
    });
  }

Texp TopLevel(const Texp& texp, const Texp& proof)
  {
    if (parseChoice(g, proof, "TopLevel") == g.shouldParseType("Def"))
      return Def(texp, proof);
    else
      return texp;
  }

Texp Def(const Texp& texp, const Texp& proof)
  {
    // def name params type do
    print("; def ", texp[0], " env\n");
    Texp this_params = Params(texp[1], proof[1]);

    Texp return_type = texp[2];
    env._current_def_return_type = &return_type;
    Texp curr_def_name = texp[0];
    env.curr_def_name = &curr_def_name;

    Texp this_def = {"def", {texp[0], this_params, texp[2], Do(texp[3], proof[3])} };

    env.curr_def_name = nullptr;
    env._current_def_return_type = nullptr;

    env._locals.clear();
    return this_def;
  }

Texp Params(const Texp& texp, const Texp& proof)
  {
    for (const auto& param : texp)
      env.addLocal(param.value, param[0].value);
    return texp;
  }

Texp Do(const Texp& texp, const Texp& proof)
  {
    Texp this_do {"do"};
    for (int i = 0; i < texp.size(); ++i)
      this_do.push(Stmt(texp[i], proof[i]));
    return this_do;
  }

Texp Stmt(const Texp& texp, const Texp& proof)
  {
    auto isName = [this](const Texp& p) {
      return parseChoice(g, p, "Value") == g.shouldParseType("Name");
    };

    print("; ", texp, "\n");

    Texp this_stmt {""};
    UnionMatch(g, "Stmt", texp, proof, {
      {"Auto",   [&](const Texp& t, const Texp& p) {
        // auto name type
        // TODO check name isn't yet taken
        auto loc = [](const std::string& s) -> std::string
          { return s + "*"; };

        env.addLocal(t[0].value, loc(t[1].value));
        this_stmt = t;
      }},
      {"Do",     [&](const Texp& t, const Texp& p) {
        this_stmt = Do(t, p);
      }},
      {"Return", [&](const Texp& t, const Texp& p) {
        // TODO check type(expr) == return type of this function
        UnionMatch(g, "Return", texp, proof, {
          {"ReturnVoid", [&](const Texp& t, const Texp& p) {
            this_stmt = t;
          }},
          {"ReturnExpr", [&](const Texp& t, const Texp& p) {
            // return value -> return value type
            Texp inferred_type = env.typeOf(g, t[0], p[0]);
            if (inferred_type.value == "int")
              inferred_type = *env._current_def_return_type;
            this_stmt = {t.value, {t[0], inferred_type} };
          }},
        });
      }},
      {"If",     [&](const Texp& t, const Texp& p) {
        // if value do
        // TODO check type(value) == i1
        this_stmt = {t.value, {t[0], Do(t[1], p[1])}};
      }},
      {"Store",  [&](const Texp& t, const Texp& p) {
        // store newValue locValue
        // -> store newValue type locValue
        // TODO check loc(type(newValue)) == type(locValue)
        if (isName(p[0].value))
          this_stmt = {t.value, {t[0], env.lookup(t[0].value), t[1]}};
        else if (isName(p[1].value))
          {
            auto unloc = [](const std::string& s) -> std::string {
              CHECK(s.ends_with('*'), s + " cannot be unloc'd if it is not a ptr");
              return s.substr(0, s.length() - 1);
            };

            this_stmt = {t.value, {t[0], unloc(env.lookup(t[1].value).value), t[1]}};
          }
        else
          {
            print("cannot yet handle non name stores:\n");
            print("  ", t, "\n");
            exit(1);
          }
      }},
      {"Call",   [&](const Texp& t, const Texp& p) {
        Texp result = Call(t, p);
        assert(result.value == "type-expr");
        this_stmt = result[1];
      }},
      {"Let",    [&](const Texp& t, const Texp& p) {
        Texp result = Expr(t[1], p[1]);
        assert(result.value == "type-expr");
        this_stmt = {t.value, {t[0], result[1]}};
        env.addLocal(t[0].value, result[0].value);
      }},
    });
    return this_stmt;
  }

Texp Expr(const Texp& texp, const Texp& proof)
  {
    auto isName = [this](const Texp& p) {
      return parseChoice(g, p, "Value") == g.shouldParseType("Name");
    };

    // TODO union-find 'int' types.
    // TODO support untypification of TypedIntLiterals

    Texp this_expr {""};
    Texp this_type {""};
    UnionMatch(g, "Expr", texp, proof, {
      {"Call",      [&](const Texp& t, const Texp& p) {
        Texp result = Call(t, p);
        assert(result.value == "type-expr");
        this_type = result[0];
        this_expr = result[1];
      }},
      {"MathBinop", [&](const Texp& t, const Texp& p) {
        // + value value -> + type value value
        Texp left_type_value = Value(t[0], p[0]);
        assert(left_type_value.value == "type-value");
        Texp left_type = left_type_value[0];
        Texp left_value = left_type_value[1];

        Texp right_type_value = Value(t[1], p[1]);
        assert(right_type_value.value == "type-value");
        Texp right_type = right_type_value[0];
        Texp right_value = right_type_value[1];

        CHECK(not(left_type.value == "int" && right_type.value == "int"), left_type.paren() + " " + right_type.paren() + ": use a typed integer literal on one side of " + texp.paren());
        Texp unify_type = left_type.value == "int" ? right_type : left_type;

        this_expr = {t.value, {unify_type, left_value, right_value}};
        this_type = unify_type;
      }},
      {"Icmp",      [&](const Texp& t, const Texp& p) {
        // > value value -> > type value value
        Texp left_type_value = Value(t[0], p[0]);
        assert(left_type_value.value == "type-value");
        Texp left_type = left_type_value[0];
        Texp left_value = left_type_value[1];

        Texp right_type_value = Value(t[1], p[1]);
        assert(right_type_value.value == "type-value");
        Texp right_type = right_type_value[0];
        Texp right_value = right_type_value[1];

        CHECK(not(left_type.value == "int" && right_type.value == "int"), left_type.paren() + " " + right_type.paren() + ": use a typed integer literal on one side of " + texp.paren());
        Texp unify_type = left_type.value == "int" ? right_type : left_type;

        this_expr = {t.value, {unify_type, left_value, right_value}};
        this_type = Texp{"i1"};
      }},
      {"Load",      [&](const Texp& t, const Texp& p) {
        // load value -> load type value
        if (isName(p[0]))
          {
            auto unloc = [](const std::string& s) -> std::string {
              CHECK(s.ends_with('*'), s + " cannot be unloc'd if it is not a ptr");
              return s.substr(0, s.length() - 1);
            };
            
            auto type = unloc(env.lookup(t[0].value).value);
            this_expr = {t.value, {type, t[0]}};
            this_type = type;
          }
        else
          {
            print("cannot handle load without a Name\n");
            exit(1);
          }
      }},
      {"Index",     [&](const Texp& t, const Texp& p) {
        // index loc-value i-value -> index loc-value type i-value
        // TODO check if loc-value is a struct then i-value must be literal
        // TODO otherwise check that i-value is of int type 

        auto unloc = [](const std::string& s) -> std::string {
          CHECK(s.ends_with('*'), s + " cannot be unloc'd if it is not a ptr");
          return s.substr(0, s.length() - 1);
        };

        auto loc = [](const std::string& s) -> std::string 
          { return s + "*"; };

        auto to_u64 = [](const std::string& s) -> size_t 
          { return std::stoull(s); };

        Texp env_type = env.lookup(t[0].value);

        if (env_type.value.starts_with("%struct."))
          {
            CHECK(env_type.value.ends_with("*"), "texp = '" + texp.paren() + "' env_type == '" + env_type.paren() + "'\n   - can only index struct pointers, try using auto-store for mutable variables\n   - currently there is no way to get the field members of a struct without loading it into an auto or malloc");
            Texp struct_type = unloc(env_type.value);
            this_expr = {t.value, {t[0], struct_type, t[1]}};
            
            Texp struct_def = env.lookup(struct_type.value);
            CHECK(struct_def.value == "struct", "obj type is " + env_type.paren() + " but env contains:\n  " + struct_def.paren());

            const auto field_count = struct_def.size();
            const auto field_index = to_u64(t[1].value) + 1;
            CHECK(field_count >= field_index, struct_def[0].value + " doesn't have enough fields to index with " + t[1].value);
            this_type = loc(struct_def[field_index][0].value);
          }
        else
          {
            // ERR struct_ptr.paren() + " in env is not a struct_ptr"
            // TODO get return type for array, after impl arrays
            CHECK(false, "cannot yet handle non struct indexing:\n   " + t.paren() + " in " + env.curr_def_name->value);
          }
      }},
      {"Cast",      [&](const Texp& t, const Texp& p) {
        // cast to-type value -> bitcast/ptrtoint/inttoptr from-type to-type value
        
        Texp type_value = Value(t[1], p[1]);
        assert(type_value.value == "type-value");
        Texp from_type = type_value[0];
        Texp to_type = t[0];
        Texp value = type_value[1];

        // TODO consider a single cast doing multiple lower level casts at once

        // TODO warn about zero extension and truncation with a flag
        std::string cast_type = ([&](){
          using namespace LLVMType;
          if (isInt(from_type.value) && isPtr(to_type.value))
            return "inttoptr";
          
          //TODO warn/ensure that pointers are cast to u64
          if (isPtr(from_type.value) && isInt(to_type.value))
            return "ptrtoint";
          
          if (isInt(from_type.value) && isInt(to_type.value))
            {
              if (getSize(from_type.value) > getSize(to_type.value))
                return "trunc";
              
              if (getSize(from_type.value) < getSize(to_type.value))
                {
                  if (isSignedInt(from_type.value) && isSignedInt(to_type.value))
                    return "sext";
                  
                  if (isUnsignedInt(from_type.value) && isUnsignedInt(to_type.value))
                    return "zext";
                  
                  CHECK(false, "must extend '" + from_type.value + "' to '" + to_type.value + "' but they are not of the same sign.");
                }
              
              return "bitcast";
            }
          
          CHECK(not (from_type.value == "int" || to_type.value == "int"), "should not cast untyped integer literals");
          CHECK(isPtr(from_type.value) && isPtr(to_type.value), "unreachable otherwise: " + from_type.value + " " + to_type.value);
          return "bitcast";
        })();

        print("; value: ", type_value, '\n');

        this_expr = {cast_type, {from_type, to_type, value} };
        this_type = t[0];
      }},
      {"Value",     [&](const Texp& t, const Texp& p) {
        print("unhandled value after normalization\n");
        exit(1);
      }},
    });
    return {"type-expr", {this_type, this_expr}};
  }

/// call name args -> call name types type args
// if it is a call-vargs, all literals must be type-qualified
// otherwise the types at the decl/def are the types expanded at the call site. 
// 
// TODO argument types must pass unification with declaration types.
Texp Call(const Texp& texp, const Texp& proof)
  {
    auto is_unqualified_literal = [](){};

    // get return-type from declaration
    Texp decl_or_def = env.lookup(texp[0].value);

    // {def, decl} name {params, types} return-type {do, .}
    Texp return_type = decl_or_def[2];

    Texp decl_types = {"types"};
    if (decl_or_def.value == "def" && decl_or_def[1].value == "params")
      for (const auto& param : decl_or_def[1])
        decl_types.push(param[0].value);
    else
      {
        CHECK(decl_or_def.value == "decl", "global that was lookup'd should either be def or decl");
        decl_types = decl_or_def[1];
      }

    Texp arg_types {"types"};
    Texp new_args {"args"};
    const Texp& args = texp[1];
    const Texp& args_proof = proof[1];
    for (int i = 0; i < args.size(); ++i)
      {
        Texp type_value = Value(args[i], args_proof[i]);
        assert(type_value.value == "type-value");
        arg_types.push(type_value[0]);
        new_args.push(type_value[1]);
      }

    Texp types {"types"};
    if (parseChoice(g, proof, "Call") == g.shouldParseType("CallVargs"))
      {
        // call-vargs
        for (const auto& arg_type : arg_types)
          {
            CHECK(arg_type.value != "int", "cannot have unqualified integer literals in call-vargs");
          }
        types = arg_types;
      }
    else
      {
        types = decl_types;
      }
    
    // TODO unify arg_types and decl_types
    for (int i = 0; i < arg_types.size(); ++i)
      { 
        // check compatibility
      }

    Texp this_call {texp.value, {texp[0], types, return_type, args}};
    return Texp {"type-expr", {return_type, this_call}};
  }

Texp Value(const Texp& texp, const Texp& proof)
  {
    if (parseChoice(g, proof, "Value") == g.shouldParseType("Literal")
      && parseChoice(g, proof, "Literal") == g.shouldParseType("IntLiteral")
      && parseChoice(g, proof, "IntLiteral") == g.shouldParseType("TypedIntLiteral")) 
      {
        // if texp is typed, remove type
        return {"type-value", {texp[0], texp.value}};
      }
    else
      {
        // otherwise it's untyped, just whack it on
        return {"type-value", {env.typeOf(g, texp, proof), texp} };
      }
  }
};
