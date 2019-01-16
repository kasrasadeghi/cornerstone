#include "grammar.h"

using Typing::Type;

std::string_view Grammar::getProduction(Type type)
  {
    std::string_view s;
    switch(type) {
    case Type::Program:       s = "(#name (* TopLevel))"; break;
    case Type::TopLevel:      s = "(| StrTable Struct Def Decl)"; break;
    case Type::StrTable:      s = "(str-table (* StrTableEntry))"; break;
    case Type::StrTableEntry: s = "(#int String)"; break;
    case Type::Struct:        s = "(struct Name (* Field))"; break;
    case Type::Field:         s = "(#name Type)"; break;
    case Type::Def:           s = "(def Name Params Type Do)"; break;
    case Type::Decl:          s = "(decl Name Types Type)"; break;
    case Type::Stmt:          s = "(| Let Return If Store Auto Do Call)"; break;
    case Type::If:            s = "(if Expr Stmt)"; break;
    case Type::Store:         s = "(store Expr Type Expr)"; break;
    case Type::Auto:          s = "(auto Name Type)"; break;
    case Type::Do:            s = "(do (* Stmt))"; break;
    case Type::Return:        s = "(| ReturnExpr ReturnVoid)"; break;
    case Type::ReturnExpr:    s = "(return Expr Type)"; break;
    case Type::ReturnVoid:    s = "(return-void)"; break;
    case Type::Let:           s = "(let Name Expr)"; break;
    case Type::Value:         s = "(| StrGet Literal Name)"; break;
    case Type::StrGet:        s = "(str-get IntLiteral)"; break;
    case Type::Literal:       s = "(| BoolLiteral IntLiteral)"; break;
    case Type::IntLiteral:    s = "(#int)"; break;
    case Type::BoolLiteral:   s = "(#bool)"; break;
    case Type::String:        s = "(#string)"; break;
    case Type::Name:          s = "(#name)"; break;
    case Type::Types:         s = "(types (* Type))"; break;
    case Type::Type:          s = "(#type)"; break;
    case Type::Params:        s = "(params (* Param))"; break;
    case Type::Param:         s = "(#name Type)"; break;
    case Type::Expr:          s = "(| Call MathBinop Icmp Load Index Cast Value)"; break;
    case Type::MathBinop:     s = "(| Add)"; break;
    case Type::Icmp:          s = "(| LT LE GT GE EQ NE)"; break;
    case Type::LT:            s = "($binop <)"; break;
    case Type::LE:            s = "($binop <=)"; break;
    case Type::GT:            s = "($binop >)"; break;
    case Type::GE:            s = "($binop >=)"; break;
    case Type::EQ:            s = "($binop ==)"; break;
    case Type::NE:            s = "($binop !=)"; break;
    case Type::Load:          s = "(load Type Expr)"; break;
    case Type::Index:         s = "(index Expr Type Expr)"; break;
    case Type::Cast:          s = "(cast Type Type Expr)"; break;
    case Type::Add:           s = "($binop +)"; break;
    case Type::Call:          s = "(| CallBasic CallVargs CallTail)"; break;
    case Type::CallBasic:     s = "(call Name Types Type Args)"; break;
    case Type::CallVargs:     s = "(call-vargs Name Types Type Args)"; break;
    case Type::CallTail:      s = "(call-tail Name Types Type Args)"; break;
    case Type::Args:          s = "(args (* Expr))"; break;
    default:  std::cout << "type not matched" << std::endl; exit(1);
    }
    return s;
  }