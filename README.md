# Pison Code Challenge

## Getting Started

### Prerequisites

To run using the included `challenge` tool, place a copy of the challenge jar in
the `aux` directory.

### Build and run

```bash
# Build the uber jar
./gradlew shadowJar

# In one terminal, start the challenge server:
tools/challenge

# And in another, start the response:
java -jar build/libs/response.jar

# The response program is fairly configurable.  For help, try:
java -jar build/libs/response.jar -h
```

## Tour

The primary code is in `src/main/kotlin/`:

- **main.kt**: Entry point and driver for the analysis chain.
- **classifiers.kt**:  Functions that transform sequences of `Sample` objects
  into sequences of activations.
- **options.kt**: Options for program/classifier behavior.
- **sequences.kt**: Utility extension methods for sequences.

A tool for running the challenge server is at `tools/challenge`.  If you'd like
to use it, be sure to place the challeng server jar in `aux/`.

## Omitted Items

Because of the one-off nature of coding challenges, some things that would
normally be present are not:

- Sophisticated signal processing.  These are just toy algorithms (though
  "gamma" works fairly well).
- Tests.  But, the code is arranged so that the important parts could be tested
  without refactoring.

## Challenge Server Improvements

There are a couple of issues with the challenge server that could be corrected
to make it easier to deal with:

- It uses a lot of CPU. Even when idle, a whole core.  At full tilt, 6-7 cores on
  my machine.
- The messages it sends frequently arrive out of order.

I believe both of these issues stem from the way messages are emitted, which
seems to use unbounded concurrency into a thread pool.  If messages are emitted
in a busy loop and scheduled onto multiple background threads for delivery (the
default behavior of many concurrency frameworks), then messages which were sent
later may jump ahead because of random selection of the next thread to run when
there are many waiters.  Additionally, slow readers can cause unbounded memory
growth in the challenge server as queued work backs up in the scheduler.

Apart from this behavior being difficult to deal with, an embedded device
emitting sensor data could never behave this way, mainly because of memory
constraints. Two more realistic behaviors for the challenge server might be:
- Send messages over UDP, always dispatching into one message queue per consumer.
- Over TCP, write via a fixed-size buffer, and when that is full, drop messages.
