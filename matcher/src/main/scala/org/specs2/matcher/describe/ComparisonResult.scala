package org.specs2.matcher.describe

import java.io.{PrintWriter, StringWriter}

import org.specs2.matcher.describe.ComparisonResultOps._
import org.specs2.text.Quote._
import org.specs2.text.NotNullStrings._

/**
 * Render the result of a comparison for different types: primitives, throwables, collections,...
 *
 * The comparison may turn out to render identical values
 * or differences
 */
sealed trait ComparisonResult {
  def render: String
}

case class PrimitiveIdentical(value: Any) extends ComparisonResult {
  def render = value.render
}

case class ThrowableIdentical(value: Throwable) extends ComparisonResult {
  def render = {
    val w = new StringWriter
    value.printStackTrace(new PrintWriter(w))
    w.toString
  }

}

case class ThrowableDifferent(result: Seq[ComparisonResult], added: Seq[StackTraceElement], removed: Seq[StackTraceElement]) extends ComparisonResult {
  def render = {
    val results = result.map(_.render)
    (results.headOption.map(_.render) ++
      results.tail.map(r => s"\n\t${r.render}"))
          .mkString
  }
}

case class StackElementIdentical(value: StackTraceElement) extends ComparisonResult {
  def render = value.toString
}

case class StackElementDifferent(className: ComparisonResult, methodName: ComparisonResult, fileName: Option[ComparisonResult], lineNumber: ComparisonResult) extends ComparisonResult {
  def render =
    s"${className.render}.${methodName.render}$renderSourceLocation"

  private def renderSourceLocation =
    fileName.map(f => s"(${f.render}:${lineNumber.render})")
            .getOrElse("(Unknown Source)")
}

case class PrimitiveDifference(actual: Any, expected: Any) extends ComparisonResult {
  def render =
    (actual, expected).renderDiff
}


case class SetIdentical(value: Set[_])
  extends OrderedCollectionIdentical(value)
  with SetTypeProvider
  with ComparisonResult

case class SetDifference(identical: Seq[Any],
                         added: Seq[Any],
                         removed: Seq[Any])
  extends UnorderedCollectionDifferent(identical, Seq.empty[Any], added, removed)
  with SetTypeProvider
  with ComparisonResult {

  protected def renderElement(element: Any) = element.render
  protected def renderChange(change: Any) = change.render
}

trait SetTypeProvider {
  val className = "Set"
}

case class SeqIdentical(value: Seq[Any])
  extends OrderedCollectionIdentical(value)
  with ComparisonResult
  with ListTypeProvider

case class SeqDifference(result: Seq[ComparisonResult], added: Seq[Any], removed: Seq[Any])
  extends OrderedCollectionDifferent(result, added, removed)
  with ComparisonResult
  with ListTypeProvider

trait ListTypeProvider {
  val className = "List"
}


case class ArrayIdentical(value: Seq[Any])
  extends OrderedCollectionIdentical(value)
  with ArrayTypeProvider
  with ComparisonResult

case class ArrayDifference(result: Seq[ComparisonResult], added: Seq[Any], removed: Seq[Any])
  extends OrderedCollectionDifferent(result, added, removed)
  with ArrayTypeProvider
  with ComparisonResult

trait ArrayTypeProvider {
  val className = "Array"
}

case class MapIdentical(m: Map[_, _])
  extends OrderedCollectionIdentical(m)
  with MapTypeProvider
  with ComparisonResult

trait MapTypeProvider {
  val className = "Map"
}

case class MapDifference(identical: Seq[(Any, Any)],
                         changed: Seq[(Any, ComparisonResult)],
                         added: Seq[(Any, Any)],
                         removed: Seq[(Any, Any)])
  extends UnorderedCollectionDifferent(identical, changed, added, removed)
  with MapTypeProvider
  with ComparisonResult {

  protected def renderElement(element: (Any, Any)) = element.render
  protected def renderChange(change: (Any, ComparisonResult)) = s"${change._1.render} -> {${change._2.render}}"
}



case class OptionIdentical(identical: Option[ComparisonResult]) extends ComparisonResult {
  def render =
    identical.map(_.render.wrapWith("Some"))
             .getOrElse("None")
}

case class EitherIdentical(identical: ComparisonResult, isRight: Boolean) extends ComparisonResult {
  def render =
    identical.render
             .wrapWith(if (isRight) "Right" else "Left")
}

case class TryIdentical(identical: Any, isSuccess: Boolean) extends ComparisonResult {
  def render =
    identical.render
             .wrapWith(if (isSuccess) "Success" else "Failure")
}

case class TryDifferent(value: ComparisonResult, isSuccess: Boolean) extends ComparisonResult {
  def render =
    value.render
         .wrapWith(if (isSuccess) "Success" else "Failure")
}

case class TryTypeDifferent(isActualSuccess: Boolean) extends ComparisonResult {
  def render =
    s"${render(isActualSuccess)} ==> ${render(!isActualSuccess)}"

  private def render(success: Boolean) = "...".wrapWith(if (success) "Success" else "Failure")

}

case class EitherDifferent(changed: ComparisonResult, isRight: Boolean) extends ComparisonResult {
  def render =
    changed.render
           .wrapWith(if (isRight) "Right" else "Left")

}

case class EitherTypeDifferent(isActualRight: Boolean) extends ComparisonResult {
  def render =
    s"${render(isActualRight)} ==> ${render(!isActualRight)}"

  private def render(right: Boolean) = "...".wrapWith(if (right) "Right" else "Left")
}

case class OptionDifferent(changed: ComparisonResult) extends ComparisonResult {
  def render =
    changed.render
           .wrapWith("Some")
}

case class OptionTypeDifferent(isActualSome: Boolean, isExpectedSome: Boolean) extends ComparisonResult {
  def render =
    s"${render(isActualSome)} ==> ${render(isExpectedSome)}"

  private def render(some: Boolean) = if (some) "Some(...)" else "None"
}


case class CaseClassPropertyComparison(fieldName: String, result: ComparisonResult, identical: Boolean)

case class CaseClassIdentical(className: String) extends ComparisonResult {
  val render = "...".wrapWith(className)
}

case class CaseClassDifferent(className: String,
                              result: Seq[CaseClassPropertyComparison])
  extends UnorderedCollectionDifferent(result.filter(_.identical), result.filterNot(_.identical), Seq.empty, Seq.empty)
  with ComparisonResult {

  protected def renderElement(element: CaseClassPropertyComparison) = renderProperty(element)
  protected def renderChange(change: CaseClassPropertyComparison)   = renderProperty(change)

  private def renderProperty(r: CaseClassPropertyComparison) = r.result.render.tagWith(r.fieldName)
}

case class OtherIdentical(actual: Any) extends ComparisonResult {
  def render = actual.render
}

case class OtherDifferent(actual: Any, expected: Any) extends ComparisonResult {

  def render = s"${actual.render(showAll = comparingPrimitiveWithObject(actual, expected))} != ${expected.render(showAll = comparingPrimitiveWithObject(actual, expected))}"

  private def comparingPrimitiveWithObject(a: Any, e: Any) = {
    val (classA, classB) = classOf(a) -> classOf(e)
    classA != classB && (isPrimitive(classA) ^ isPrimitive(classB))
  }

  private def isPrimitive(clazz: String) = clazz.startsWith("java.lang.")

  private def classOf(v: Any) =
    Option(v).map(_.getClass.getName)
             .getOrElse("null")
}




abstract class OrderedCollectionIdentical(value: Iterable[Any]) {
  def render =
    value.map(_.render)
         .mkString(", ")
         .wrapWith(className)

  def className: String
}


abstract class UnorderedCollectionDifferent[Element, Change](identical: Seq[Element],
                                            changed: Seq[Change],
                                            added: Seq[Element],
                                            removed: Seq[Element]) {

  def render: String =
    Seq(renderIdentical ++ renderChanged ++ renderAdded ++ renderRemoved)
      .flatten
      .mkString(", ")
      .wrapWith(className)

  private def renderIdentical =
    identical.toOption
             .map(_.map(renderElement)
                    .mkString(", "))

  private def renderChanged =
    changed.toOption
           .map(_.map(renderChange)
                  .mkString(", "))

  private def renderAdded =
    added.toOption
         .map(_.map(renderElement)
                .mkString(", ")
                .tagWith("added"))

  private def renderRemoved =
    removed.toOption
           .map(_.map(renderElement)
                  .mkString(", ")
                  .tagWith("removed"))

  def className: String
  protected def renderElement(element: Element): String
  protected def renderChange(change: Change): String
}

abstract class OrderedCollectionDifferent[Element](result: Seq[ComparisonResult],
                                                   added: Seq[Element],
                                                   removed: Seq[Element]) {
  def render: String =
    Seq(renderResult ++ renderAdded ++ renderRemoved)
      .flatten
      .mkString(", ")
      .wrapWith(className)

  private def renderResult =
    result.toOption
          .map(_.map(_.render)
          .mkString(", "))

  private def renderAdded =
    added.toOption
         .map(_.map(_.render)
                       .mkString(", ")
                .tagWith("added"))

  private def renderRemoved =
    removed.toOption
           .map(_.map(_.render)
                  .mkString(", ")
                  .tagWith("removed"))

  def className: String
}

object ComparisonResultOps {

  case class PropertyDifference[A, E](actual: A, expected: E)

  implicit class `Any -> String`(private val value: Any) extends AnyVal {

    def render(showAll: Boolean): String =
      value match {
        case v if showAll => v.notNullWithClass(showAll = true)
        case (k, v) => s"${k.render} -> ${v.render}"
        case x: String => q(x)
        case x => unq(x)
      }

    def render: String = render(showAll = false)
  }

  implicit class `Difference -> String`(private val diff: (Any, Any)) extends AnyVal {
    def renderDiff: String =
      s"${diff._1.render} != ${diff._2.render}"
  }

  implicit class ClassesOps(private val values: String) extends AnyVal {
    def wrapWith(`type`: String): String =
      s"${`type`}($values)"

    def tagWith(tag: String): String =
      s"$tag: $values"
  }

  implicit class SeqOps[T](private val s: Seq[T]) extends AnyVal {
    final def toOption: Option[Seq[T]] = if (s.isEmpty) None else Some(s)
  }
}
