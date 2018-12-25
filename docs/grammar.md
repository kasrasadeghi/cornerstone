
/// IMPLEMENTATIONS

// (#int String)

// (struct Name (* Field))
// ($isStruct)

// ("Name" Type)
  //TODO add to struct lookup

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
  //TODO add as parameter to closest defun ancestor


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


(Grammar
  (Program       (#name (* TopLevel)))
  (TopLevel      (| StrTable Struct Def Decl))
  (StrTable      (str-table (* StrTableEntry)))
  (StrTableEntry (#int String))
  (Struct        (struct Name (* Field)))
  (Field         (#name Type))
  (Def           (def Name Params Type Do))
  (Decl          (decl Name Types Type))
  (Stmt          (| Let Return If Store Auto Do Call))
  (If            (if Expr Stmt))
  (Store         (store Expr Type Expr))
  (Auto          (auto Name Type))
  (Do            (do (* Stmt)))
  (Return        (| ReturnExpr ReturnVoid))
  (ReturnExpr    (return Expr Type))
  (ReturnVoid    (return-void))
  (Let           (let Name Expr))
  (Value         (| StrGet Literal Name))
  (StrGet        (str-get IntLiteral))
  (Literal       (| BoolLiteral IntLiteral))
  (IntLiteral    (#int))
  (BoolLiteral   (#bool))
  (String        (#string))
  (Name          (#name))
  (Types         (types (* Type)))
  (Type          (#type))
  (Params        (params (* Param)))
  (Param         (#name Type))
  (Expr          (| Call MathBinop Icmp Load Index Cast Value))
  (MathBinop     (| Add))
  (Icmp          (| LT LE GT GE EQ NE))
  (LT            ($binop <))
  (LE            ($binop <=))
  (GT            ($binop >))
  (GE            ($binop >=))
  (EQ            ($binop ==))
  (NE            ($binop !=))
  (Load          (Load Type Expr))
  (Index         (index Expr Type Expr))
  (Cast          (cast Type Type Expr))
  (Add           ($binop +))
  (Call          (| CallBasic CallVargs CallTail))
  (CallBasic     (call Name Types Type Args))
  (CallVargs     (call-vargs Name Types Type Args))
  (CallTail      (call-tail Name Types Type Args))
  (Args          (args (* Expr)))
)