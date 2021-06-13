## Effex

Pronounced "Ef-Ex" - you can guess that I chose the name before writing any
code.

Exploration of using [Cats Effect 3](https://typelevel.org/cats-effect/) and [fs2](https://fs2.io/) with [ScalaFX](https://github.com/scalafx/scalafx).

To get a feel for what the code looks like, check out the
[demos](modules/demo/src/main/).

## Running

The demos should be cross-platform and self-contained, just do:

```scala
sbt> demo/run
```

And choose which demo to run.

## Why?

There's no particular good reason.

Several concepts from CE3 and FS2 seemed useful:

1. `Dispatcher[IO]` - safe construct for submitting arbitrary `IO` actions to
   the runtime

   In a lot of scenarios in ScalaFX/JavaFX it's impossible to express yourself
   in IO-context - the side-effects are very much the core of the design.

   Dispatcher, along with some syntactic sugar, helps interacting with a
   well-formed, referentially transparent IO-core of program logic (executing
   in the background on IO runtime).

2. `Supervisor[IO]` - ability to align arbitrary fibers' lifetimes with the
   lifetime of Supervisor - which itself is a `Resource`, initiated at the start
   of the application

3. `SignallingRef[IO, A]` from fs2 - a nice, safe concurrent state (like `Ref`
   from CE) with the added benefit of notifying subscribers to changes.

  
    This allows instantiating the data model's state safely before the GUI is
    rendered, and interact with it in a thread safe manner.

    With a sprinkle of syntactic sugar, it is possible to populate ScalaFX
    properties from the changes in the `Ref`, and post the changes of ScalaFX
    properties into the components stored in the Ref.

    Resulting in a (potentially) a purely asynchronous state management with
    ScalaFX interface.


