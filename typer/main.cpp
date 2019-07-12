#include "parser.h"
#include "texp.h"
#include "print.h"
#include "matcher.h"
#include "macros.h"
#include "io.h"

#include <sstream>
#include <fstream>

using Types = std::vector<std::pair<std::string, std::string>>;

struct Env {
  Types globals;
  Types locals;
};

auto operator<<(std::ostream& o, const Env& env) -> std::ostream& 
  {
    int count = 0;
    for (auto [name, type] : env.locals)
      {
        if (count++ != 0) o << ", ";
        o << name << "->" << type;
      }
    return o;
  }


// region build_env

namespace build_env {

auto params_types(const Grammar& g, Types& types, Texp& params) -> std::string;
auto process_stmt(const Grammar& g, Env& env, Texp& stmt, Texp& proof) -> void;
auto type_of_expr(const Grammar& g, Env& env, Texp& expr, Texp& proof) -> std::string;

auto params_types(const Grammar& g, Types& types, Texp& params) -> std::string 
  {
    std::stringstream ss;
    for (auto& param : params) 
      {
        types.emplace_back(param.value, param[0].value);
        ss << param << " ";
      }
    ss << '\n';
    return ss.str();
  }

auto process_stmt(const Grammar& g, Env& env, Texp& stmt, Texp& proof) -> void 
  {
    UnionMatch(g, "Stmt", stmt, proof, {
      {"Let",    [&](const auto& texp, const auto& p) { env.locals.emplace_back(stmt[0].value, type_of_expr(g, env, stmt[1], proof[1])); }},
      {"Return", [&](const auto& texp, const auto& p) { return; }},
    });
  }

auto type_of_expr(const Grammar& g, Env& env, Texp& expr, Texp& proof) -> std::string 
  {
    std::string type;
    UnionMatch(g, "Expr", expr, proof, {
      {"Call",    [&](const auto& texp, const auto& p) { type = expr[2].value; }},
    });
    CHECK(type != "", "type not found in type_of_expr");
    return type;
  }
}
// endregion build_env

int main(int argc, char* argv[]) 
  {
    if (argc != 2) 
      {
        print("usage: typer <filename.bb>");
        exit(1);
      }

    Texp prog = parse_from_file(argv[1]);
    Grammar g { parse_from_file("docs/bb-grammar.texp")[0] };
    Matcher m { g };

    auto proof = ([&]() {
      auto optional_proof = m.is(prog, "Program");
      if (not optional_proof) 
        {
          print("grammar error with ", argv[1]);
          exit(1);
        }
      else
        return *optional_proof;
    })();

    Env env;

    using namespace build_env;
    
    for (int i = 0; i < prog.size(); ++i)
      {
        auto& tl = prog[i];
        auto& tl_proof = proof[i];
        if (proof_type(g, tl_proof, "TopLevel") == CHECK_UNWRAP(g.parseType("Def"), "Def not in grammar"))
          {
            params_types(g, env.locals, tl[1]);
            print('(', tl.value, ' ', tl[0], ' ', tl[1], ' ', tl[2], " (do\n");
            for (int i = 0; i < tl[3].size(); ++i)
              {
                print("  // ", env, "\n");
                auto& stmt = tl[3][i];
                auto& stmt_proof = tl_proof[3][i];
                process_stmt(g, env, stmt, stmt_proof);
                print("  ", stmt, '\n');
              }
            print("))", "\n\n");
          }
      }

    // NOTE: We need to be able to get the type of certain expressions. 
    // To do that, we might require some context, e.g. the program for the global scope and the function definition for the local scope.
    // We would say, 
    // - "what is the type of this expression after this statement in this function?"
    //   - "how does this statement affect the environment after the statements before it?"
    // - "what is the type of this function?"
    // - "what is the type of this global variable?"
    // 
    // We can accumulate results from the statements in a sequence using an type environment, which is a simple look up table for the
    // value names to their types.
  }