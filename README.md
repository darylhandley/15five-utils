# 15Five Utils Shell

A simple interactive command-line shell application built with Kotlin and Maven. This shell serves as the foundation for future integration with the 15Five application.

## Features

- **Interactive Shell**: Command-line interface with colorized output for better readability
- **Color-coded Interface**: 
  - Blue prompt (`15five>`)
  - Green success messages
  - Red error messages  
  - Yellow command highlights
  - Visual separators between commands
- **Built-in Commands**:
  - `echo <message>` - Echo the message back to the shell
  - `help` - Display available commands
  - `quit` / `exit` - Exit the shell

## Project Structure

```
15five-utils/
├── pom.xml                                    # Maven configuration
├── README.md                                  # This file
└── src/main/kotlin/com/sonatype/darylhandley/fifteenfiveutils/
    └── ShellApp.kt                           # Main application file
```

## Prerequisites

- **Java 11+** - Required for Kotlin compilation and execution
- **Maven 3.6+** - Build system and dependency management
- **Terminal with ANSI color support** - For best visual experience

## Building and Running

### Compile the Application

```bash
mvn compile
```

### Run the Shell

```bash
mvn exec:java
```

### Alternative: Compile and Run in One Command

```bash
mvn compile exec:java
```

## Usage

Once the shell starts, you'll see:

```
15Five Utils Shell - Type 'help' for commands or 'quit' to exit
────────────────────────────────────────────────────────────
15five> 
```

### Example Session

```
15five> help
Available commands:
  echo <message>  - Echo the message back
  help           - Show this help
  quit/exit      - Exit the shell
──────────────────────────────
15five> echo Hello World!
Hello World!
──────────────────────────────
15five> quit
Goodbye!
```

## Technical Details

- **Language**: Kotlin 1.9.10
- **Build System**: Maven
- **Package**: `com.sonatype.darylhandley.fifteenfiveutils`
- **Main Class**: `ShellAppKt`
- **Java Target**: 11

## Future Plans

This shell will be extended to interact with the 15Five application, providing command-line utilities for:
- Performance review management
- Goal tracking
- Team communication
- Reporting and analytics

## Development Notes

The application uses ANSI escape codes for terminal colors. If you're running in an environment that doesn't support ANSI colors, you may see escape sequences in the output instead of colored text.

## Maven Configuration

The project is configured with:
- Kotlin Maven plugin for compilation
- Exec Maven plugin for easy execution
- Java 11 compatibility
- Proper source directory configuration for Kotlin files 
