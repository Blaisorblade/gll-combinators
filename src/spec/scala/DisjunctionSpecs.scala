import edu.uwm.cs.gll._

import org.specs._
import org.scalacheck._

object DisjunctionSpecs extends Specification with ImplicitConversions with ScalaCheck {
  import Prop._
  
  "disjunctive parser" should {
    "gather binary alternatives" in {
      val prop = forAll { (left: String, right: String) =>
        (left | right).asInstanceOf[DisjunctiveParser[String]].gather == Set(literal(left), literal(right))
      }
      
      prop must pass
    }
    
    "compute FIRST for binary alternatives" in {
      val prop = forAll { (left: String, right: String) =>
        val leftFirst = if (left.length == 0) Set[Char]() else Set(left charAt 0)
        val rightFirst = if (right.length == 0) Set[Char]() else Set(right charAt 0)
        
        (left | right).first == leftFirst ++ rightFirst
      }
      
      ("" | "").first mustEqual Set()
      
      prop must pass
    }
    
    "parse binary alternatives" in {
      {
        val p = "daniel" | "chris"
        
        p("daniel" toStream) must beLike {
          case Success("daniel", Stream()) :: Nil => true
          case _ => false
        }
        
        p("chris" toStream) must beLike {
          case Success("chris", Stream()) :: Nil => true
          case _ => false
        }
      }
      
      {
        val p = "" | ""
        
        p("" toStream) must beLike {
          case Success("", Stream()) :: Nil => true
          case _ => false
        }
      }
    }
    
    "gather nary alternatives" in {
      def check(p: Parser[Any], expected: Parser[String]*) {
        p.asInstanceOf[DisjunctiveParser[String]].gather mustEqual Set(expected:_*)
      }
      
      {
        val p = "daniel" | "chris" | "joseph"
        check(p, "daniel", "chris", "joseph")
      }
      
      {
        val p = "daniel" | "daniel" | "chris" | "joseph"
        check(p, "daniel", "chris", "joseph")
      }
      
      {
        val p = "" | "chris" | "" | "daniel" | "daniel"
        check(p, "", "daniel", "chris")
      }
    }
    
    "compute FIRST for nary alternatives" in {
      def check(p: Parser[Any], expected: String*) {
        val set = expected.foldLeft(Set[Char]()) { (set, str) =>
          if (str.length == 0) set
          else set + str.charAt(0)
        }
        
        p.first == set
      }
      
      {
        val p = "daniel" | "chris" | "joseph"
        check(p, "daniel", "chris", "joseph")
      }
      
      {
        val p = "daniel" | "daniel" | "chris" | "joseph"
        check(p, "daniel", "chris", "joseph")
      }
      
      {
        val p = "" | "chris" | "" | "daniel" | "daniel"
        check(p, "", "daniel", "chris")
      }
    }
    
    "parse nary alternatives" in {
      // assumes unambiguous data
      def check(p: Parser[Any], data: String*) = {
        for (str <- data) {
          p(str toStream) must beLike {
            case Success(str, Stream()) :: Nil => true
            case _ => false
          }
        }
      }
      
      {
        val p = "daniel" | "chris" | "joseph" | "renee" | "bethany" | "grace"
        check(p, "daniel", "chris", "joseph", "renee", "bethany", "grace")
      }
      
      {
        val p = "renee" | "bethany" | "grace"
        check(p, "renee", "bethany", "grace")
      }
      
      {
        val p = "daniel" | "chris" | "joseph" | "renee"
        check(p, "daniel", "chris", "joseph", "renee")
      }
    }
    
    "map results" in {
      val prop = forAll { (left: String, right: String, f: String=>Int) =>
        left != right ==> {
          val p = (
              left
            | right
          ) ^^ f
          
          (p(left toStream) match {
            case Success(v, Stream()) :: Nil => v == f(left)
            case _ => false
          }) && (p(right toStream) match {
            case Success(v, Stream()) :: Nil => v == f(right)
            case _ => false
          })
        }
      }
      
      prop must pass
    }
    
    "handle binary shift/reduce ambiguity" in {
      val prop = forAll { (head: String, suffix: String) =>
        val p = head ~ suffix | (head + suffix)
        val result = p((head + suffix) toStream)
        
        val a = {
          result exists {
            case Success(`head` ~ `suffix`, Stream()) => true
            case _ => false
          }
        }
        
        val b = {
          val v = head + suffix
          
          result exists {
            case Success(`v`, Stream()) => true
            case x => false
          }
        }
        
        a && b
      }
      
      val p = "" | ""
      p("" toStream) mustHave {
        case Success("", Stream()) => true
        case _ => false
      }
      
      prop must pass
    }
    
    "compute FIRST for left-recursive grammar" in {
      def p: Parser[Any] = p | "a"
      
      p.first mustEqual Set('a')
    }
  }
}
