
/// IMPLEMENTATIONS

// (#int String)

// (struct Name (* Field))
// ($isStruct)

// ("Name" Type)
// ($isField)
    //TODO
    // - name regexp?
    // - register name/type for struct?

    // TODO return type name // TODO add to union-find functionality
    // TODO function name // TODO add to namespace functionality
// (def FuncName Params ReturnType Do)

// (decl FuncName Types ReturnType)
    // TODO function name namespace
// (call FuncName Types ReturnType Args)
// (call-vargs FuncName Types ReturnType Args)
// (call-tail FuncName Types ReturnType Args)
// (| Let Return If Store Auto Do Call)
    //TODO localname namespace
// (let LocalName Expr/(not Value))
// (if Expr/Value Do) //TODO second do? for else branch?
// (| ReturnExpr ReturnVoid)

// (return Expr/Value ReturnType)
// (return-void)
// (store ValueExpr/Value Type LocationExpr/Value/Name/AutoName?)
// TODO local namespace
// TODO type to allocate
// (auto LocalName Type)
  // { return match(t, "(auto Name Type)"); }

// (do (* Stmt))
// (load Type LocExpr/Value)
  // { return match(t, "(load Type Expr)"); }

// (index PtrExpr Type IntExpr/IntValue)
  // { return match(t, "(index Expr Type Expr)"); }

// (cast FromType ToType Expr/Value)
  // { return match(t, "(cast Type Type Expr)"); }

// (| StrGet Literal Name)
// (| IntLiteral BoolLiteral)

// (#bool)
// ($isBoolLiteral)

// (#string)

// (types (* Type))
// ($isType)

// (params (* Param))

//TODO (Name Type)
// ($isParam)

// (str-get IntLiteral)

// (| Add)

// ($binop +)
// ($binop <)
// ($binop <=)
// ($binop >)
// ($binop >=)
// ($binop ==)
// ($binop !=)

// (| LT LE GT GE EQ NE)
// (args (* Expr))
