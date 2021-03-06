import scala.collection.immutable.HashMap
import scala.util.matching.Regex
import Crypt._
import java.io.PrintWriter
import java.io.File

case class Entry ( account   : String
                 , password  : String
                 , uid       : Int
                 , gid       : Int
                 , gecos     : String
                 , directory : String
                 , shell     : String
                 )

object Entry {

  def apply(line: String) : Entry = {
    val splits = line.split(":")
    Entry (splits(0), splits(1), splits(2).toInt, splits(3).toInt,
           splits(4), splits(5), splits(6))
  }

}

object Crack {

  val digitDict = HashMap("o" -> "0", "i" -> "1", "l" -> "1", "z" -> "2",
                          "e" -> "3", "a" -> "4", "s" -> "5", "b" -> "68",
                          "t" -> "7", "g" -> "9", "q" -> "9",
                          "O" -> "0", "I" -> "1", "L" -> "1", "Z" -> "2",
                          "E" -> "3", "A" -> "4", "S" -> "5", "B" -> "68",
                          "T" -> "7", "G" -> "9", "Q" -> "9")

  def transformReverse(w: String) : Iterator[String] = {
    Iterator(w, w.reverse)
  }

  def transformCapitalize(w: String) : Iterator[String] = {
    if (w.length == 0) {
      Iterator("")
    } else {
      for (c <- Iterator(w(0).toString, w(0).toString.toUpperCase());
           s <- transformCapitalize(w.substring(1)))
        yield(c + s)
    }
  }

  def transformDigits(w:String) : Iterator[String] = {
    if (w.length == 0) {
      Iterator("")
    } else {
      val head = w(0).toString
      val headTransforms = head + digitDict.getOrElse(head, "")
      val headTransformsArray = headTransforms.toList.map(_.toString)
      val headTransformsIterator = headTransformsArray.toIterator
      for (c <- headTransformsIterator; s <- transformDigits(w.substring(1)))
        yield(c + s)
    }
  }

  def checkPassword(plain: String, enc: String) : Boolean =
    Crypt.crypt(enc, plain) == enc

  def candidateWords(file: String) =
    Words.wordsMatchingRegexp(file, new Regex("""^.{6,8}$"""))

  // scala> Crack("passwd", "words", "soln.1")
  // goto> scala Crack passwd words soln.1
  def apply(pwdFile: String, wordsFile: String, outFile: String) : Unit = {
    // read pwdFile
    val targets = Lines.list(pwdFile).map(Entry.apply)
    var remainingHashes = for (t <- targets) yield t.password
    var hashToUser = HashMap[String, String]()
    for (t <- targets)
      hashToUser = hashToUser + (t.password -> t.account)
    // init outFile writer
    val writer = new PrintWriter(new File(outFile))

    // crack non-transformed
    val cracker_naive =
      for (w <- candidateWords(wordsFile))
        yield w
    val cracker_reverse =
      for (a <- candidateWords(wordsFile))
        yield a.reverse
    val cracker_digit =
      for (a <- candidateWords(wordsFile);
           w <- transformDigits(a))
        yield w
    val cracker_capital =
      for (a <- candidateWords(wordsFile);
           w <- transformCapitalize(a))
        yield w
    val cracker_reverse_digit =
      for (a <- candidateWords(wordsFile);
           w <- transformDigits(a.reverse))
        yield w
    val cracker_reverse_capital =
      for (a <- candidateWords(wordsFile);
           w <- transformCapitalize(a.reverse))
        yield w
    val cracker_digit_capital =
      for (a <- candidateWords(wordsFile);
           b <- transformDigits(a);
           w <- transformCapitalize(b))
        yield w
    val cracker_reverse_digit_capital =
      for (a <- candidateWords(wordsFile);
           b <- transformDigits(a.reverse);
           w <- transformCapitalize(b))
        yield w

    val crackers = Iterator(cracker_naive,
                            cracker_reverse,
                            cracker_digit,
                            cracker_reverse_capital,
                            cracker_capital,
                            cracker_reverse_digit,
                            cracker_digit_capital,
                            cracker_reverse_digit_capital)

    for (cracker <- crackers) {
      println("### new cracker applied")
      println(remainingHashes)
      for (word <- cracker) {
        for (hash <- remainingHashes) {
          if (checkPassword(word, hash)) {
            println(hashToUser(hash) + "=" + word)
            writer.write(hashToUser(hash) + "=" + word + "\n")
            writer.flush()
            remainingHashes = remainingHashes.filter(h => h != hash)
          }
        }
        if (remainingHashes.isEmpty) {
          writer.close()
          return
        }
      }
    }

    // clean up
    writer.close()
  }

  def main(args: Array[String]) = {
    println("Begin: Cracking Passwords")
    apply(args(0), args(1), args(2))
    println("Done: Cracking Passwords")
  }
}

// vim: set ts=2 sw=2 et:
