import edu.uwm.cs.gll._

import org.specs._
import org.scalacheck._

object ArithmeticSpecs extends Specification with ScalaCheck with RegexParsers {
  import Prop._
  import StreamUtils._
  
  "arithmetic grammar" should {
    "compute FIRST set" in {
      val first = expr.first
      
      if (first.size != 0) {
        Set('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-') forall { c =>
          first contains c mustBe true
        }
      } else
        first mustEqual Set()
    }
    
    "parse numbers" in {
      val prop = forAll { x: Int =>
        (abs(x.toLong) < Math.MAX_INT) ==> {
          expr(x.toString) match {
            case Success(e, LineStream()) #:: SNil => e.solve == x
            case _ => false
          }
        }
      }
      
      prop must pass
    }
    
    "parse simple addition" in {
      val prop = forAll { (x: Int, y: Int) =>
        (abs(x.toLong) < Math.MAX_INT && abs(y.toLong) < Math.MAX_INT) ==> {
          val res = expr((x + "+" + y))
          
          if (x < 0) {
            val res1 = res.length == 2
            
            val res2 = res exists {
              case Success(e @ Add(Neg(e1), e2), LineStream()) => e.solve == x + y
              case _ => false
            }
            
            val res3 = res exists {
              case Success(e @ Neg(Add(e1, e2)), LineStream()) => e.solve == -(-x + y)
              case _ => false
            }
            
            res1 && res2 && res3
          } else {
            res match {
              case Success(e, LineStream()) #:: SNil => e.solve == x + y
              case _ => false
            }
          }
        }
      }
      
      prop must pass
    }
    
    "parse simple subtraction" in {
      val prop = forAll { (x: Int, y: Int) =>
        (abs(x.toLong) < Math.MAX_INT && abs(y.toLong) < Math.MAX_INT) ==> {
          val res = expr((x + "-" + y))
          
          if (x < 0) {
            val res1 = res.length == 2
            
            val res2 = res exists {
              case Success(e @ Sub(Neg(e1), e2), LineStream()) => e.solve == x - y
              case _ => false
            }
            
            val res3 = res exists {
              case Success(e @ Neg(Sub(e1, e2)), LineStream()) => e.solve == -(-x - y)
              case _ => false
            }
            
            res1 && res2 && res3
          } else {
            res match {
              case Success(e, LineStream()) #:: SNil => e.solve == x - y
              case _ => false
            }
          }
        }
      }
      
      prop must pass
    }
    
    "parse simple multiplication" in {
      val prop = forAll { (x: Int, y: Int) =>
        (abs(x.toLong) < Math.MAX_INT && abs(y.toLong) < Math.MAX_INT) ==> {
          val res = expr((x + "*" + y))
          
          if (x < 0) {
            val res1 = res.length == 2
            
            val res2 = res exists {
              case Success(e @ Mul(Neg(e1), e2), LineStream()) => e.solve == x * y
              case _ => false
            }
            
            val res3 = res exists {
              case Success(e @ Neg(Mul(e1, e2)), LineStream()) => e.solve == -(-x * y)
              case _ => false
            }
            
            res1 && res2 && res3
          } else {
            res match {
              case Success(e, LineStream()) #:: SNil => e.solve == x * y
              case _ => false
            }
          }
        }
      }
      
      prop must pass
    }
    
    "parse simple division" in {
      val prop = forAll { (x: Int, y: Int) =>
        (abs(x.toLong) < Math.MAX_INT && abs(y.toLong) < Math.MAX_INT && y != 0) ==> {
          val res = expr((x + "/" + y))
          
          if (x < 0) {
            val res1 = res.length == 2
            
            val res2 = res exists {
              case Success(e @ Div(Neg(e1), e2), LineStream()) => e.solve == x / y
              case _ => false
            }
            
            val res3 = res exists {
              case Success(e @ Neg(Div(e1, e2)), LineStream()) => e.solve == -(-x / y)
              case _ => false
            }
            
            res1 && res2 && res3
          } else {
            res match {
              case Success(e, LineStream()) #:: SNil => e.solve == x / y
              case _ => false
            }
          }
        }
      }
      
      prop must pass
    }
    
    "produce both associativity configurations" in {
      val res = expr("42 + 13 + 12") map { 
        case Success(e, LineStream()) => e
        case r => fail("%s does not match the expected pattern".format(r))
      }
      
      val target = Set(Add(IntLit(42), Add(IntLit(13), IntLit(12))),
                       Add(Add(IntLit(42), IntLit(13)), IntLit(12)))
      
      Set(res:_*) mustEqual target
    }
    
    "produce both binary precedence configurations" in {
      val res = expr("42 + 13 - 12") map { 
        case Success(e, LineStream()) => e
        case r => fail("%s does not match the expected pattern".format(r))
      }
      val target = Set(Add(IntLit(42), Sub(IntLit(13), IntLit(12))),
                       Sub(Add(IntLit(42), IntLit(13)), IntLit(12)))
      
      Set(res:_*) mustEqual target
    }
    
    "produce both unary precedence configurations" in {
      val res = expr("-42 + 13") map {
        case Success(e, LineStream()) => e.solve
        case r => fail("%s does not match the expected pattern".format(r))
      }
      
      (res sort { _ < _ } toList) mustEqual List(-55, -29)
    }
  }
  
  def abs(i: Int) = Math.abs(i)
  
  def abs(l: Long) = Math.abs(l)
  
  // %%
  
  lazy val expr: Parser[Expr] = (
      expr ~ ("+" ~> expr)  ^^ Add
    | expr ~ ("-" ~> expr)  ^^ Sub
    | expr ~ ("*" ~> expr)  ^^ Mul
    | expr ~ ("/" ~> expr)  ^^ Div
    | "-" ~> expr           ^^ Neg
    | num                   ^^ IntLit
  )
  
  val num = """\d+""".r     ^^ { _.toInt }
  
  // %%
  
  sealed trait Expr {
    val solve: Int
  }
  
  case class Add(e1: Expr, e2: Expr) extends Expr {
    val solve = e1.solve + e2.solve
  }
  
  case class Sub(e1: Expr, e2: Expr) extends Expr {
    val solve = e1.solve - e2.solve
  }
  
  case class Mul(e1: Expr, e2: Expr) extends Expr {
    val solve = e1.solve * e2.solve
  }
  
  case class Div(e1: Expr, e2: Expr) extends Expr {
    val solve = e1.solve / e2.solve
  }
  
  case class Neg(e: Expr) extends Expr {
    val solve = -e.solve
  }
  
  case class IntLit(i: Int) extends Expr {
    val solve = i
  }
}