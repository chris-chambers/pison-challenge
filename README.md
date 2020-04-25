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
```
