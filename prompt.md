# Project: Real-Time Chat Application

Act as a senior full-stack software engineer and software architect. Build a complete, responsive, real-time chat application using Spring Boot, React, WebSocket (STOMP), MongoDB, and Redis.

Do not build just a UI prototype. Implement a functional application with a real backend, persistent message history, real-time messaging, authentication, and proper error handling.

## 1. Technology Stack

### Frontend
- React.js with Vite and TypeScript
- Tailwind CSS
- shadcn/ui for reusable components
- React Router for navigation
- Zustand for client-side state management
- TanStack Query for REST API state and caching
- STOMP.js and SockJS for WebSocket communication
- Axios for HTTP requests
- React Hook Form and Zod for form validation
- Lucide React for icons
- date-fns for timestamps

### Backend
- Java 21
- Spring Boot 3.x
- Spring Web
- Spring Security
- Spring Data MongoDB
- Spring WebSocket with STOMP
- Spring Validation
- Spring Boot Actuator
- Maven

### Data and infrastructure
- MongoDB for users, conversations, memberships, and message history
- Redis for online presence, ephemeral state, and cross-instance pub/sub where required
- Docker and Docker Compose for local infrastructure
- JWT-based authentication
- Environment variables for all secrets and configuration

Use mutually compatible, stable dependency versions. Do not mix incompatible Spring Boot, Spring Security, or Java versions.

## 2. Product Requirements

Build a polished messaging platform inspired by modern messaging applications, without copying any brand's proprietary design.

### Authentication
- Registration with name, email, and password
- Login and logout
- Password hashing with BCrypt
- JWT access-token validation
- Protected routes and backend endpoints
- Display name and profile avatar
- Proper validation and user-friendly authentication errors

Use a secure token strategy. Prefer an HttpOnly, Secure cookie for refresh-token handling, with appropriate SameSite and CSRF protections. Do not store long-lived authentication tokens in localStorage. Configure CORS and cookie settings explicitly.

### Main application layout
Create a three-part desktop layout:
1. Left sidebar containing the user's profile, search, conversation list, and New Chat button.
2. Central panel containing the active conversation, message history, typing indicator, and message composer.
3. Optional right-side details panel containing group members, conversation information, and shared media.

On mobile, use a responsive conversation-list and chat-screen navigation pattern.

### One-to-one messaging
- Start a direct conversation with another registered user.
- Send and receive messages in real time.
- Show sender name, avatar, timestamp, and message status.
- Distinguish outgoing and incoming messages.
- Show the latest message in the conversation list.
- Prevent duplicate direct conversations between the same two users.

### Group messaging
- Create group conversations.
- Set a group name and avatar.
- Add and remove members with appropriate permissions.
- Display member lists.
- Support real-time group messages.
- Show the sender's identity for messages in group conversations.
- Restrict group management operations to authorized members.

### Message history
- Persist messages in MongoDB.
- Load the latest messages when a conversation opens.
- Implement cursor-based pagination for older messages.
- Preserve chronological display order.
- Prevent duplicate messages during pagination and WebSocket updates.
- Show a loading state while older messages are fetched.
- Handle deleted or unavailable conversations gracefully.

### Presence and typing
- Track online/offline presence using Redis with a configurable TTL.
- Update presence through authenticated connection lifecycle events and heartbeats.
- Clean up stale presence when clients disconnect or their TTL expires.
- Use Redis pub/sub when presence events must be distributed across multiple backend instances.
- Show online indicators in the conversation list and active chat.
- Broadcast typing-start and typing-stop events.
- Automatically stop typing indicators after a timeout.
- Avoid broadcasting sensitive information.

### Message states
Support:
- Sending
- Sent
- Delivered, where recipient delivery acknowledgements are implemented
- Read, where read receipts are implemented

Clearly distinguish server acceptance from recipient delivery and read confirmation. Do not label a message as delivered or read without the corresponding acknowledgement.

### Search and usability
- Search registered users by name or email, without exposing passwords or private account fields.
- Search the current user's conversations.
- Provide empty states, loading skeletons, retry actions, and error messages.
- Add unread message counts and mark conversations as read.
- Keep the chat view automatically scrolled to the latest message when appropriate, without disrupting users reading older messages.
- Add keyboard support: Enter to send, Shift+Enter for a new line, Escape to close dialogs.
- Prevent accidental duplicate submissions.

## 3. UI and Visual Design

Create a premium, clean messaging interface.

Visual direction:
- Minimal, modern SaaS interface.
- Neutral backgrounds, subtle borders, restrained shadows, and consistent spacing.
- Use a coherent color system and accessible contrast.
- Use rounded message bubbles with different styling for sent and received messages.
- Display avatars with initials when images are unavailable.
- Include subtle hover states and transitions.
- Use skeleton loaders instead of blank content during loading.
- Support light and dark themes.
- Ensure the interface works at desktop, tablet, and mobile breakpoints.
- Use accessible labels, focus states, and keyboard navigation.

Screens:
1. Login
2. Registration
3. Main chat workspace
4. New direct conversation
5. Create group dialog
6. Group details and member management
7. Profile/settings
8. Not-found and error states

Do not use mock buttons that do nothing. Every visible action must be implemented or clearly marked as intentionally unavailable.

## 4. Backend Architecture

Organize the backend using clear package boundaries:

com.example.chat
- config
- security
- auth
- user
- conversation
- message
- websocket
- presence
- common
- exception

Use controllers for HTTP endpoints, services for business logic, repositories for persistence, and DTOs for API boundaries.

Separate domain models from request and response DTOs. Avoid exposing MongoDB documents directly to clients.

Implement:
- Global exception handling
- Request validation
- Consistent API error responses
- Structured logging
- Authentication and authorization checks
- Pagination
- MongoDB indexes
- Unit and integration tests
- Health checks

Do not put all logic in controllers. Keep the code modular, testable, and maintainable.

## 5. REST API Requirements

Implement approximately eight core REST API operations, organized by resource. Additional endpoints are permitted when needed.

Authentication:
- POST /api/auth/register
- POST /api/auth/login
- POST /api/auth/logout

Users:
- GET /api/users/me
- GET /api/users/search?q={query}

Conversations:
- GET /api/conversations
- POST /api/conversations/direct
- POST /api/conversations/group
- GET /api/conversations/{conversationId}
- POST /api/conversations/{conversationId}/members
- DELETE /api/conversations/{conversationId}/members/{userId}

Messages:
- GET /api/conversations/{conversationId}/messages?before={cursor}&limit=30
- POST /api/conversations/{conversationId}/read

The HTTP POST message endpoint is optional if message sending is handled exclusively through authenticated WebSocket messaging. If both transports are supported, ensure they share the same authorization, persistence, and validation rules.

Use appropriate HTTP status codes and consistent JSON responses. Document the endpoints with OpenAPI/Swagger.

## 6. WebSocket Architecture

Use STOMP over WebSocket, with SockJS fallback if required.

Configure an endpoint such as:
- /ws

Define three primary application message destinations:
- /app/chat.send
- /app/chat.typing
- /app/chat.read

Define user-specific or conversation-specific subscriptions, such as:
- /user/queue/messages
- /user/queue/receipts
- /topic/conversations/{conversationId}
- /topic/conversations/{conversationId}/typing

Implement authentication during the WebSocket connection and validate authorization for every subscription and message publication. Never trust a conversation ID supplied by a client without verifying membership.

For every outgoing message:
1. Validate the authenticated sender.
2. Validate conversation membership.
3. Validate message content and length.
4. Persist the message to MongoDB.
5. Publish the persisted message to authorized recipients.
6. Return the server-generated message ID and timestamp.
7. Update conversation preview and unread counts safely.

Use stable message IDs and idempotency keys where appropriate. Ensure unauthorized users cannot subscribe to private conversations or impersonate another sender.

Keep the three primary application channels clearly documented. Distinguish application destinations from broker destinations and WebSocket endpoints.

## 7. MongoDB Data Model

Design collections for:

### User
- id
- displayName
- email
- passwordHash
- avatarUrl
- createdAt
- updatedAt

### Conversation
- id
- type: DIRECT or GROUP
- name
- avatarUrl
- memberIds
- createdBy
- lastMessage
- lastMessageAt
- createdAt
- updatedAt

### Message
- id
- conversationId
- senderId
- content
- type: TEXT or SYSTEM
- createdAt
- editedAt
- deletedAt
- clientMessageId

Use appropriate indexes for email uniqueness, conversation membership queries, and message pagination. Design direct-conversation uniqueness carefully so concurrent requests cannot create duplicate conversations.

Avoid embedding an unbounded message history in a conversation document.

## 8. Redis Design

Use Redis for ephemeral presence and distributed events.

Suggested keys:
- presence:user:{userId}
- presence:connection:{connectionId}

Store presence with TTL and refresh it through heartbeats. Support multiple sessions per user so closing one browser tab does not mark the user offline while another session remains connected.

Use Redis pub/sub for presence events across application instances if necessary. Do not use Redis as the permanent source of truth for message history.

For multiple backend instances, document WebSocket broker delivery limitations and implement a suitable shared-broker or relay strategy if required. Redis pub/sub alone must not be assumed to distribute messages to every STOMP client connected to different application instances.

## 9. Security Requirements

- Hash passwords using BCrypt.
- Authenticate REST endpoints and WebSocket connections.
- Authorize all conversation access and membership changes.
- Validate STOMP SEND and SUBSCRIBE destinations.
- Validate and limit message size.
- Apply rate limits to login, registration, and message submission.
- Configure CORS with explicit allowed origins.
- Protect cookie-based authentication against CSRF.
- Never commit secrets or production credentials.
- Avoid leaking sensitive data in logs or error responses.
- Validate and sanitize displayed user-generated content.
- Use HTTPS/WSS in production.
- Prevent insecure direct object reference vulnerabilities.
- Do not allow users to read another user's private conversation history.

## 10. Testing

Create meaningful tests for:
- Registration and login
- Password hashing
- JWT authentication and authorization
- Direct-conversation creation and duplicate prevention
- Group membership permissions
- Message persistence
- Message pagination
- Unauthorized conversation access
- WebSocket send and subscription authorization
- Redis presence TTL and multi-session behavior
- Message delivery acknowledgements
- Error handling

Use JUnit 5, Mockito, Spring Boot Test, and Testcontainers where practical. Add frontend tests for authentication forms, chat state, message rendering, and WebSocket event handling.

Provide instructions to run all tests locally.

## 11. Docker and Environment Configuration

Provide:
- Frontend Dockerfile
- Backend Dockerfile
- docker-compose.yml
- MongoDB service with persistent development storage
- Redis service
- Health checks
- .env.example
- Backend application configuration
- Frontend API and WebSocket configuration

Never commit real secrets. Keep development defaults separate from production settings.

The application should start with documented commands, ideally:
docker compose up --build

Document any additional steps needed to create the initial user accounts or run database migrations/index initialization.

## 12. Deliverables

Generate a complete repository containing:
- React frontend
- Spring Boot backend
- MongoDB repositories and indexes
- Redis presence service
- WebSocket configuration and handlers
- Authentication and authorization
- DTOs, services, and controllers
- Unit and integration tests
- Docker configuration
- .env.example
- README.md
- Architecture diagram
- API documentation
- Sample environment setup instructions

The README must explain prerequisites, startup commands, environment variables, architecture, REST endpoints, WebSocket destinations, test execution, troubleshooting, and deployment considerations.

## 13. Development Workflow

Work in phases rather than generating the entire application in one unverified step.

Phase 1: Inspect the repository, establish the architecture, define the data model, and create a detailed implementation plan.

Phase 2: Scaffold the Spring Boot backend, configure MongoDB and Redis, and implement authentication.

Phase 3: Implement users, direct conversations, groups, and REST APIs.

Phase 4: Implement authenticated STOMP messaging, message persistence, subscriptions, typing indicators, and receipts.

Phase 5: Implement Redis-backed presence and multi-session handling.

Phase 6: Build the React UI and integrate every screen with the backend.

Phase 7: Add Docker Compose, tests, API documentation, and README.

Phase 8: Run builds and tests, fix all errors, verify real-time communication between two separate user sessions, and validate authorization boundaries.

At each phase:
- List the files to be created or modified.
- Implement working code, not pseudocode.
- Run the available build and test commands.
- Fix errors before moving to the next phase.
- Report completed work, remaining issues, and exact commands for verification.
- Never claim a test passed unless it was actually executed successfully.
- Do not overwrite existing project files without inspecting them first.

Start with Phase 1. Inspect the existing repository and present the architecture and implementation plan. Then proceed phase by phase, implementing and verifying the application.