package org.specs2
package guide

object Structure extends UserGuidePage {
  def is = s2"""

### Styles

In a Specification you generally want to include 2 things:

 - some informal text describing what the system/application/function should do
 - some code specifying exactly what is expected when something is executed

With ***specs2*** you have 2 main ways to do this:

 - you can create an "Acceptance" specification where all the informal text is written in one place and the code is written elsewhere. The name "acceptance" comes from the fact that it might be easier for a non-developer to read to text to validate your specification

 - you can create a "Unit" specification where the code is interleaved with the text. The name "unit" comes from the fact that Unit specifications have a structure which is close to unit tests in "classical" frameworks such as JUnit

Both ways of writing specifications have advantages and drawbacks:

 - Acceptance specifications are easier to read as a narrative but require navigation between the text and the code. You also need to define a `is` method holding the body of the specification.
 - Unit specifications are easier to navigate but the text tends to be lost in a sea of code

#### Acceptance specification

An acceptance specification extends `org.specs2.Specification` and defines the `is` method. You can implement this method with an interpolated **`s2`** string: ${
    snippet {
      class MySpecification extends org.specs2.Specification {
        def is = s2"""

 this is my specification
   where example 1 must be true           $e1
   where example 2 must be true           $e2
                                          """

        def e1 = 1 must_== 1

        def e2 = 2 must_== 2
      }
    }
  }

The `s2` string contains the text of your specification as well as some references to methods (`e1` and `e2`) defining *`Results`*. When the Specification is executed the `s2` string is analysed and 2 `Examples` are created then executed:

 - one `Example` with the description "where example 1 must be true" and the code `1 must_== 1`
 - another `Example` with the description "where example 2 must be true" and the code `2 must_== 2`

All the rest, `"this is my specification"`, is parsed as `Text` and is not executed.

#### Unit specification

A unit specification extends `org.specs2.mutable.Specification` and uses the `>>` operator to create "blocks" containing `Texts` and `Examples`: ${
    snippet {
      class MySpecification extends org.specs2.mutable.Specification {
        "this is my specification" >> {
          "where example 1 must be true" >> {
            1 must_== 1
          }
          "where example 2 must be true" >> {
            2 must_== 2
          }
        }
      }
    }
  }

This specification creates one piece of `Text` and 2 `Examples` as before but:

 - there is no need to define an `is` method (this means that a mutable variable is used to collect the `Texts` and `Examples` hence the `mutable` package)
 - the code is close to each piece of text it specifies

However once a specification is created with all its `Texts` and `Examples`, the execution will be the same, whether it is an Acceptance one or a Unit one.

The `>>` blocks can be nested and this allows you to structure your specification so that the outermost blocks describe the general context while the innermost ones describe a more specific context. A similar effect can be achieved by simply indenting text in an acceptance specification.

### Expectations

There is another major difference between the acceptance specifications and unit specifications. The first style encourages you to write [one expectation per example](http://bit.ly/one_assertion_per_test) while the second allows to use several. One expectation per example is useful because when a specification fails, you know immediately what is wrong. However it is sometimes expensive to setup data for an example so having several expectations sharing the same setup is sometimes what you want.

The good news is that for each of the 2 main styles, acceptance and unit, you can choose exactly which mode you prefer if the default mode is not convenient.

#### Functional expectations

In an acceptance specification, by default, the `Result` of an `Example` is always given by the last statement of its body. For instance, this example will never fail because the first expectation is "lost": ${
    snippet {
      // this will never fail!
      s2"""
  my example on strings $e1
"""
      def e1 = {
        // because this expectation will not be returned,...
        "hello" must have size (10000)
        "hello" must startWith("hell")
      }
    }
  }

If you want to get both expectations you will need to use `and` between them: ${
    snippet {
      s2"""
  my example on strings $e1
"""
      def e1 = ("hello" must have size (10000)) and
        ("hello" must startWith("hell"))
    }
  }

This is a bit tedious and not very pleasing to read so you can see why this mode encourages one expectation per example only. If you need several expectations per example, you can need to mix-in the `org.specs2.execute.ThrownExpectations` trait to the specification which is the one used for unit specifications by default.

##### Thrown expectations

With a unit specification you get "thrown expectations". When an expectation fails, it throws an exception and the rest of the example is not executed: ${
    snippet {
      class MySpecification extends org.specs2.mutable.Specification {
        "This is my example" >> {
          1 must_== 2 // this fails
          1 must_== 1 // this is not executed
        }
      }
    }
  }

It is also possible to use the "functional" expectation mode with a unit specification by mixing in the `org.specs2.execute.NoThrownExpectations` trait.

### Now learn how to...

 - use ${"matchers" ~/ Matchers} to specify the body of your examples
 - set up ${"contexts" ~/ Contexts} for the examples
 - control the ${"execution" ~/ Execution} of a specification
 - ${"run" ~/ Runners} a specification

### And if you want to know more

 - create ${"*auto-examples*" ~ AutoExamples} where the code *is* the description of the `Example`
 - integrate ${"snippets of code" ~ CaptureSnippets} to your specification
 - ${"skip" ~ SkipExamples} examples
 - collect ${"*all* expectations" ~ GetAllExpectations}
 - use ${"named examples" ~ NamedExamples} in acceptance specifications to get default example names
 - use ${"scripts and auto-numbered examples" ~ AutoNumberedExamples} to completely separate the specification text from the code
 - use the Given-When-Then style for structuring specifications
 - mark examples as ${"pending until they are fixed" ~ PendingUntilFixedExamples}
 - add ${"links to other specifications" ~ LinkOtherSpecifications}
 - use the ${"command line arguments" ~ UseCommandLineArguments} to define the body of an example
 - create a trait which will add specification fragments before or after an other specification when mixed-in

"""
}
/*


#### DataTables

[DataTables](org.specs2.guide.Matchers.html#DataTables) are generally used to pack lots of expectations inside one example. A DataTable which is used as a `Result` in the body of an Example will only be displayed when failing. If, on the other hand you want to display the table even when successful, to document your examples, you can omit the example description and inline the DataTable directly in the specification: ${snippet{

  class DataTableSpec extends Specification with Tables { def is =

    "adding integers should just work in scala"  ^ eg {
      "a"   | "b" | "c" |
        2    !  2  !  4  |
        1    !  1  !  2  |>
        { (a, b, c) =>  a + b must_== c }
    }
  }
}}
This specification will be rendered as:
```adding integers should just work in scala
+  a | b | c |
   2 | 2 | 4 |
   1 | 1 | 2 |
```
#### Example groups

When you create acceptance specifications, you have to find names to reference your examples, which can sometimes be a bit tedious. You can then get some support from the `${fullName[specification.Grouped]}` trait. This trait provides group traits, named `g1` to `g22` to define groups of examples. Each group trait defines 22 variables named `e1` to `e22`, to define examples bodies. The specification below shows how to use the `Grouped` trait: ${snippet{

  class MySpecification extends Specification with Examples { def is =  s2"""
  first example in first group                                        ${g1.e1}
  second example in first group                                       ${g1.e2}

  first example in second group                                       ${g2.e1}
  second example in second group                                      ${g2.e2}
  third example in second group, not yet implemented                  ${g2.e3}
  """
  }

  trait Examples extends specification.Grouped with matcher.Matchers {
    // group of examples with no description
    new g1 {
      e1 := ok
      e2 := ok
    }
    // group of examples with a description for the group
    "second group of examples" - new g2 {
      e1 := ok
      e2 := ok
    }
  }
}}

Note that, if you use groups, you can use the example names right away, like `g2.e3`, without providing an implementation, the example will be marked as `Pending`.

}


*/