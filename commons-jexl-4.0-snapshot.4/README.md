<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
Apache Commons JEXL Pro
=======================

The Apache Commons JEXL Pro library is an experimental fork of the The Apache Commons JEXL library.

Idea of the fork
----------------
The fork is intended to be source compatible with the latest JEXL version (3.2-SNAPSHOT), but provides some 
enhancements and changes to the capabilities of the scripting language. 

I have no intention of promoting this fork as an alternative implementation, and I would be happy to have all 
the changes to be backported to the base JEXL library one day, but the decision whether these changes are the ones 
the JEXL community would benefit from remains at the descretion of the Apache JEXL team.

Language Compatibility 
----------------------
The library tends to maintain as much syntax compatibility with the original syntax as possible, but there are
some changes that may break your existing scripts. The main reason for this comes from the introduction of the new 
reserved words to support new syntax constructs, so your variables may no longer be named by one of those that were introduced. 
There are also some minor tweaks to the original syntax in order to clarify language structure and align language 
constructs with other popular scripting languages, to minimize the learning curve. 
These changes are all reflected in the documentation, but the breef summary is given below.

Incompatible changes
--------------------
+ Java 8 is the new minimum supported version 

+ New reserved words are introduced. Those are:
  `switch` `case` `default` `try` `catch` `finally` `throw` `synchronized` `this` `instanceof` `in` `remove`
  `assert` `final` `boolean` `char` `byte` `short` `int` `long` `float` `double`.
  You may not longer use them as the names of the variables and methods. The exception is made for the `remove` identifer,
  as it is still may be used in method invocations.

+ Pragmas can only be defined at the beginning of the script. The reason is the pragmas are not executable constructs, 
  so it is pointless and misleading to have them incorporated in flow-control structures somewhere in the middle.

+ Literal `null` can not have any properies, so it is forbidden to use it in expressions like `null.prop`.
  If, for some reason, you still want to do this, use parentheses like `(null).prop`.

+ Precedence of the `range` operator (`..`) is changed to be higher than that of relational operators, 
  but lower than that of arithmetic operators.

+ Precedence of the `match` operator (`=~`) and `not-match` operator (`!~`) is changed to be that of other equality operators.

+ Passing a function more arguments than is specified in the function declaration now results in error

+ Hoisted variables are effectively final. The reason is hoisted variables are in fact copies of the original variables,
  so assigning a new value to the hoisted variable does not affect the original one. Such behaviour is misleading and thus restricted.

+ Object methods `x.empty()` and `x.size()` are treated like ordinary method calls and are no longer equivalent to operators `empty(x)` and `size(x)`

+ Left-hand assignment expression can not use safe access operator `?.`

New features
------------
+ Java-like `switch` statement is introduced

+ Java-like `synchronized` statement is introduced

+ Java-like `try-with-resources` statement is introduced

+ Java-like `try-catch-finally` statement is introduced

+ Java-like `throw` statement is introduced

+ Java-like `for` classical loop statement is introduced

+ Java-like `assert` statement is introduced

+ New `remove` flow-control statement is introduced

+ New `this` literal is introduced to allow easier access to the current evaluation context

+ Java-like `<<`,`>>`,`>>>` bitwise shift operators are introduced 

+ Java-like `<<=`,`>>=`,`>>>=` self-assignment operators are introduced 

+ Java-like `++` and `--` increment/decrement operators are introduced. Prefix and postfix forms are supported

+ Java-like `instanceof` operator is introduced

+ Groovy-like `!instanceof` operator is introduced

+ Groovy-like `?=` conditional assignment operator is introduced

+ Java-like `::` method reference operator is introduced

+ Java-like `switch` expression operator is introduced

+ Java-like `()` type-cast operator is introduced

+ Javascript-like `===` and `!==` identity operators are introduced

+ C-like `&` pointer and `*` pointer dereference operators are introduced

+ C#-like `?[]` safe array access operator is introduced

+ New Map.Entry literal `[a : b]` is introduced

+ New block evaluation `({})` operator is introduced

+ New iterator `...` operator is introduced

+ New iterator processing (selection/projection/reduction) operators are introduced

+ New multiple assignment statement `(x,y) = [2,3]` is introduced

+ New inline property assignment `a{b:3,c:4}` construct is introduced

Enhancements
------------
+ Labeled blocks and statements like `switch`, `for`, `while`, `do`, `if`, `try`, `synchronized` can be used. 
  The defined labels can be further specified for inner `break`, `continue` and `remove` flow-control statements

+ Multidimensional arrays can be accessed by using new syntax `arr[x,y]` as well as by using older syntax `arr[x][y]`

+ Array element assignment operator `x[0] = value` now tries to perform implicit type cast of the assigned value

+ Single expression functions can be defined by using `=>` fat arrow operator without outer curly braces

+ Variable argument functions can be defined by using `...` syntax after the last function argument

+ Functions now implement almost all basic java8 `@FunctionalInterface` interfaces, 
  so it is possible to pass a function as an argument to java methods that accept such interfaces

+ Function parameters can use `var` keyword for parameter definition

+ Function parameters can be declared strongly typed by using java primitive types `function(int a, int b) {a+b}`

+ Function parameters can be declared as `final`

+ Function parameters can be declared as non-null `function(var &x)`

+ Return statement expression can be omitted, implying `null` as a result

+ Local variables can be declared strongly typed by using java primitive types `int i = 0`

+ Local variables can be declared final `final var i = 0`

+ Local variables can be declared as non-null `var &i = 0`

+ Last part of the ternary expression `x?y:z` (along with the separating `:`) can be omitted, implying `null` as a result

+ Pattern matching operators `=~` and `!~` can use new `in` and `!in` aliases 

+ Operator `new` supports Java-like object creation syntax `new String()` or array creation syntax `new String[] {'abc','def'}`

+ Operator `new` supports Java-like inner object creation syntax `outerObject.new InnerClass()`

+ Foreach statement may also define additional `counter` variable `for (var x,i : list)` along with the current loop value variable

+ Immutable list `#[1,2,3]` literal constructs can be used 

+ Immutable set `#{1,2,3}` literal constructs can be used

+ Immutable map `#{1:2,3:4}` literal constructs can be used

+ Array comprehensions `[...a]` can be used in array literals

+ Set comprehensions `{...a}` can be used in set literals

+ Map comprehensions `{*:...a}` can be used in map literals

+ Function argument comprehensions `func(...a)` can be used 

+ Binary format `0b...` for natural literals 

+ Java-like support for underscores in numeric literals

+ Corresponding unicode characters may be used for the operators like `!=`, `>=` etc

License
-------
This code is under the [Apache Licence v2](https://www.apache.org/licenses/LICENSE-2.0).

See the `NOTICE.txt` file for required notices and attributions.
