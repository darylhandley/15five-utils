# 15Five Utils Shell - Project Context

## Project Overview
A Kotlin-based interactive command-line shell application for interacting with the 15Five API. Built with Maven, featuring professional CLI capabilities and colorized output.

## Architecture

### Package Structure
```
src/main/kotlin/com/sonatype/darylhandley/fifteenfiveutils/
├── ShellApp.kt                    # Main application with JLine3 CLI
├── client/
│   └── FifteenFiveClient.kt      # Feign HTTP client interface
├── model/
│   └── User.kt                   # User data class matching 15Five API
├── service/
│   └── UserService.kt            # Business logic for user operations
└── util/
    ├── ConfigLoader.kt           # Properties file configuration loader
    └── TableFormatter.kt         # ASCII table formatting utility
```

## Technology Stack
- **Language**: Kotlin 1.9.10
- **Build System**: Maven
- **HTTP Client**: Feign (OpenFeign 13.1)
- **JSON Processing**: Jackson 2.16.1 with Kotlin module
- **CLI Features**: JLine3 3.24.1 (command history, tab completion, line editing)
- **Table Formatting**: AsciiTable 0.3.2
- **Target JVM**: Java 11

## Configuration System
- **Properties Files**:
  - `application.properties` (gitignored) - Contains actual session ID and CSRF token
  - `application.properties.sample` (committed) - Template configuration file
- **Configuration Fields**:
  - `fifteen.five.session.id` - 15Five session cookie for API authentication
  - `fifteen.five.csrf.token` - CSRF token for objective creation/cloning

## Available Commands
- `help` - Show available commands and shell features
- `echo <message>` - Echo message back to console
- `users list` - List all users from 15Five API
- `users list <search>` - Search users by name or title (no quotes needed)
- `objectives list` - List top 100 objectives
- `objectives list <limit>` - List objectives with custom limit
- `objectives listbyuser <id>` - List objectives for user ID or alias
- `objectives get <id>` - Get single objective by ID
- `objectives clone <id> <user>` - Clone objective to another user
- `useralias create <alias> <userid>` - Create user alias
- `useralias list` - List all user aliases
- `useralias remove <alias>` - Remove user alias
- `quit` / `exit` - Exit the shell

## Shell Features (via JLine3)
- **Command History**: ↑/↓ arrow navigation through previous commands
- **Tab Completion**: Auto-complete available commands
- **Line Editing**: ←/→ arrows, Home/End, Backspace/Delete
- **Graceful Exit**: Ctrl+C handling

## API Integration
- **Base URL**: `https://sonatype.15five.com`
- **Authentication**: Cookie-based using `sessionid`
- **Endpoint**: `/account/company/users/` with query parameters
- **Response Format**: Direct JSON array of user objects (not wrapped)

### User Model Fields
```kotlin
data class User(
    val id: Int,
    val globalId: String,
    val fullName: String,
    val title: String?,
    val avatarUrl: String?,
    val isReviewer: Boolean,
    val reviewerId: Int?,
    val reviewerFullName: String?,
    val reviewerGlobalId: String?,
    val isActive: Boolean
)
```

## Color Scheme
- **Blue**: Command prompts (`15five>`)
- **Green**: Success messages and table output
- **Red**: Error messages
- **Yellow**: Command highlights and exit messages
- **Cyan**: Headers and titles
- **Dim**: Separators and secondary information

## Build & Run Commands
```bash
# Compile
mvn compile

# Run application
mvn exec:java

# Compile and run
mvn compile exec:java
```

## Development Notes
- **Session ID**: Obtain from browser cookies after logging into 15Five
- **CSRF Token**: Obtain from objectives create form for cloning functionality
- **Error Handling**: Currently crashes on errors (by design for simplicity)

## Testing & Debugging
- **ObjectiveCloneTest**: Standalone test for troubleshooting objective cloning
  - Tests cloning objective 12872510 to user alias "daryl"
  - Provides detailed debugging output
  - Run with: `mvn exec:java@test-clone`
- **Search**: Filters by user's `fullName` and `title` fields
- **Table Output**: Shows User ID, Full Name, Title, and Active status
- **Command Parsing**: Simple string-based matching with `when` expressions

## File Structure
```
15five-utils/
├── .gitignore                    # Excludes application.properties
├── pom.xml                       # Maven configuration with all dependencies  
├── README.md                     # Project documentation
├── sample.properties             # Configuration template
├── application.properties        # Actual config (gitignored)
└── src/main/
    ├── kotlin/...                # Source code (see package structure above)
    └── resources/sample_json/
        └── users.json            # Sample API response for reference
```

## Recent Enhancements
1. **Added JLine3**: Upgraded from basic `readLine()` to professional CLI with history and completion
2. **Fixed API Models**: Updated User model to match actual 15Five API response structure  
3. **Simplified Search**: Removed quote requirement for search terms (`users list john` vs `users list "john"`)
4. **Enhanced Table**: Added Title and Active status columns to user listing
5. **Better Help**: Added shell features documentation to help command

## Future Extension Points
- Additional 15Five API endpoints (goals, reviews, etc.)
- More sophisticated command parsing
- Configuration management commands
- Export capabilities
- Batch operations

## Dependencies in pom.xml
- `kotlin-stdlib`
- `feign-core`, `feign-jackson`, `feign-okhttp`
- `jackson-core`, `jackson-databind`, `jackson-module-kotlin`
- `jline-terminal`, `jline-reader`
- `asciitable`

## Development Workflow
The application uses a simple compile-run cycle. No hot reload currently implemented - manual restart required for code changes. The session ID is configured once in `application.properties` and reused across shell sessions.