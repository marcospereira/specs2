package org.specs2
package matcher.describe

import scala.util.{Failure, Try}
import PrimitiveDiffable.primitive

/**
 * Typeclass for values which can be compared and return a comparison result
 */
trait Diffable[-T]{

  def diff(actual: T, expected: T): ComparisonResult

}

object Diffable extends DiffableLowPriority1 {

  def diff[T](actual: T, expected: T)(implicit di: Diffable[T]): ComparisonResult =
    di.diff(actual, expected)
}

trait DiffableLowPriority1 extends DiffableLowPriority2 {
  // instances for primitive types
  implicit val intDiffable    : Diffable[Int]     = primitive
  implicit val booleanDiffable: Diffable[Boolean] = primitive
  implicit val stringDiffable : Diffable[String]  = primitive
  implicit val longDiffable   : Diffable[Long]    = primitive
  implicit val floatDiffable  : Diffable[Float]   = primitive
  implicit val doubleDiffable : Diffable[Double]  = primitive

  // basic elements
  implicit val stackTraceElementDiffable: Diffable[StackTraceElement] = new StackTraceElementDiffable
  implicit val exceptionDiffable: Diffable[Throwable] = new ThrowableDiffable

  //scala objects
  implicit def optionDiffable[T : Diffable]: Diffable[Option[T]] = new OptionDiffable[T]
  implicit val optionNoneDiffable: Diffable[Option[Nothing]] = OptionNoneDiffable

  implicit def eitherRightDiffable[R : Diffable]: Diffable[Right[Nothing, R]] = new EitherRightDiffable[R]
  implicit def eitherLeftDiffable[L : Diffable]: Diffable[Left[L, Nothing]] = new EitherLeftDiffable[L]

  implicit def tryDiffable[T : Diffable]: Diffable[Try[T]] = new TryDiffable[T]
  implicit val failureDiffable: Diffable[Failure[Nothing]] = new FailureDiffable


  // scala collections
  implicit def mapDiffable[K : Diffable, V : Diffable]: Diffable[Map[K, V]] = new MapDiffable[K, V]
  implicit def setDiffable[E: Diffable]: Diffable[Set[E]] = new SetDiffable
  implicit def seqDiffable[E: Diffable]: Diffable[Seq[E]] = new SeqDiffable
  implicit def arrayDiffable[E: Diffable]: Diffable[Array[E]] = new ArrayDiffable
}

trait DiffableLowPriority2 {
  implicit def eitherDiffable[L : Diffable, R : Diffable]: Diffable[Either[L, R]] = new EitherDiffable[L, R]
  implicit def fallbackDiffable[T]: Diffable[T] = new FallbackDiffable[T]
}


