# 15Five Utils Shell - Project Context

## Project Overview
A Kotlin-based interactive command-line shell application for interacting with the 15Five API. Built with Maven, featuring professional CLI capabilities and colorized output.

## Architecture

### Package Structure
```
src/main/kotlin/com/sonatype/darylhandley/fifteenfiveutils/
├── ShellApp.kt                    # Main application with JLine3 CLI
├── auth/
│   ├── AuthService.kt            # Validate / save / swap session; clipboard read
│   ├── SessionStore.kt           # ~/.15fiveutils/session (600), legacy application.properties fallback
│   └── SessionExpiredException.kt
├── client/
│   ├── FifteenFiveHttp.kt        # Shared OkHttp + Feign stack: cookie/CSRF interceptor, no redirects, expiry detection
│   ├── FifteenFiveClient.kt      # Feign read endpoints
│   └── FifteenFiveFormClient.kt  # Form POSTs (update key result, no-op write probe)
├── model/
│   └── User.kt                   # User data class matching 15Five API
├── service/
│   └── UserService.kt            # Business logic for user operations
└── util/
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

## Authentication
- Official 15Five Public API keys are Account-Admin-only and company-wide, so this app uses 15Five's internal web endpoints with the browser `sessionid` cookie.
- Session stored in `~/.15fiveutils/session` (chmod 600) via the `auth` command (clipboard or inline, validated before saving). Legacy fallback: `fifteen.five.sessionid` in `./application.properties`.
- CSRF: self-generated random token per run, sent as `ff_csrf_token` cookie + `X-CSRFToken` header + `csrfmiddlewaretoken` form field. Django double-submit check is not session-bound — verified: no token → 403, matching self-generated token → passes CSRF, mismatched → 403.
- `FifteenFiveHttp` disables redirect-following (OkHttp **and** Feign `Request.Options`, which otherwise re-enables it). Expired session = 302 to `/?next=…` (pages, users endpoint) or 401/403 (JSON endpoints) → `SessionExpiredException` → shell prints "run 'auth'".
- Create objective (`/objectives/create/`) succeeds with a 302 to `/objectives/details/<id>/`; the new ID is parsed from `Location`. A 200 means the form re-rendered with validation errors.

## Available Commands
- `help` - Show available commands and shell features
- `auth` / `auth <sessionid>` - Save + verify the 15Five session (clipboard by default)
- `auth status` - Check session (read) and write path (no-op CSRF probe)
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
- **Session ID**: Copy the `sessionid` cookie from the browser, run `auth`
- **CSRF Token**: Generated automatically — nothing to copy
- **Error Handling**: Command errors are printed; an expired session prints a re-auth hint

## Testing & Debugging
- **ObjectiveCloneTest**: Standalone test for troubleshooting objective cloning
  - Tests cloning objective 12872510 to user alias "daryl"
  - Provides detailed debugging output
  - Run with: `mvn exec:java@test-clone` (needs a saved session — run `auth` in the shell first)
- **Search**: Filters by user's `fullName` and `title` fields
- **Table Output**: Shows User ID, Full Name, Title, and Active status
- **Command Parsing**: Simple string-based matching with `when` expressions

## File Structure
```
15five-utils/
├── .gitignore                    # Excludes application.properties
├── pom.xml                       # Maven configuration with all dependencies  
├── README.md                     # Project documentation
├── application.properties        # Legacy session fallback only (gitignored)
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
The application uses a simple compile-run cycle. No hot reload currently implemented - manual restart required for code changes. The session ID is saved once via `auth` to `~/.15fiveutils/session` and reused across shell sessions until it expires.