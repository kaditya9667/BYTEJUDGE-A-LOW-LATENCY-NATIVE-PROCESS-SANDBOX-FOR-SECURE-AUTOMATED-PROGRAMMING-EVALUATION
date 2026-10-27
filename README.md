# JavaJudge — LeetCode-Style Online Judge (Java 21 & C++ 20)

A high-performance **LeetCode-Style Remote Code Execution (RCE) Online Judge & Sandbox** built with **Spring Boot 3.2.5**, supporting both **Java 21 LTS** and **C++ 20 (GCC 16)**. Features include static security policy enforcement, live Server-Sent Events (SSE) streaming, submission history & percentile tracking, dynamic testcase evaluation with hidden suites, and an interactive Monaco Editor workbench with automated error repair (**AutoFix**).

---

## ⚡ Key Capabilities

### 1. Multi-Language Support: Java & C++
- **Java 21 LTS:** Compiles with `javac` and executes with `-Xmx64M` JVM heap cap.
- **C++ 20:** Compiles with `g++ -O3 -std=c++20` (MinGW-W64 GCC 16) and executes isolated binaries with standard stream draining.
- **Instant Language Switching:** Seamlessly toggles editor syntax highlighting, starter code, file extensions (`Solution.java` vs `solution.cpp`), and compiler toolchains.
- *Note:* **No extra C++ installation is needed on your PC** — MinGW GCC 16 is already detected, installed, and verified on your system.

### 2. LeetCode-Grade Problem Solving & Method Harness
- **Method Signature Harness:** Users can write either pure LeetCode classes (without `main`) or full standalone programs with custom I/O — the backend automatically injects driver wrappers if `main()` is omitted!
- **Preloaded DSA Problems:**
  - *Two Sum* (Easy)
  - *Valid Parentheses* (Easy)
  - *Palindrome Number* (Easy)
  - *Maximum Subarray* (Kadane's Algorithm, Medium)
  - *Climbing Stairs* (Dynamic Programming, Easy)
- **Dual Execution Modes:**
  - **Run Code (`Ctrl + Enter`):** Evaluates solution against sample testcases or arbitrary custom input without penalty.
  - **Submit (`Ctrl + Shift + Enter`):** Evaluates against all testcases (including hidden suites) and returns official verdicts:
    - `ACCEPTED` (All testcases passed)
    - `WRONG_ANSWER` (Output mismatch)
    - `TIME_LIMIT_EXCEEDED` (Infinite loop guard, > 5.0s)
    - `SECURITY_VIOLATION` (Forbidden system call / exploit attempt)
    - `COMPILATION_ERROR` (Compiler diagnostics)
    - `RUNTIME_ERROR` (Process crash or unhandled exception)

### 3. Hardened Sandbox Security Policy
- Static security analysis (`SecurityValidator.java`) inspects submitted source code before execution:
  - **Java:** Blocks `System.exit()`, `Runtime.getRuntime()`, `ProcessBuilder`, reflection (`java.lang.reflect`), network sockets (`java.net.Socket`), and disk manipulation (`java.io.File`, `java.nio.file.Files`).
  - **C++:** Blocks `system()`, `fork()`, `exec()`, `popen()`, network headers (`<windows.h>`, `<winsock2.h>`, `<sys/socket.h>`), and file destruction (`remove()`, `rename()`).
  - Violations immediately trigger a **`SECURITY_VIOLATION`** verdict without executing unverified system code.

### 4. Submission History & LeetCode Percentile Analytics
- **Submissions Tab:** Browse past submissions with verdict pills, runtimes (ms), languages, timestamps, and a **"Load Code"** button to restore past attempts into the editor.
- **Speed Percentile:** Calculates runtime distribution among accepted submissions:
  - e.g., *"Runtime: 2 ms (Beats 91.2% of users)"*.

### 5. Freeform Playground Scratchpad
- Switch to **Playground Mode** for an unrestricted Java and C++ scratchpad.
- Built-in templates for Hello World, Vector/Array Sorting, Fibonacci, Multithreading, TLE Watchdog, and Security checks.
- Full interactive standard input (`stdin`) and real-time terminal output.

---

## 🏗️ Architecture

```
                                  User Action (Web Browser)
                       +-----------------------------------------------+
                       |  Monaco Code Editor & LeetCode Console        |
                       |  - Language: Java 21 / C++ 20                 |
                       |  - [Run Code]       -> POST /api/submissions  |
                       |  - [Submit]         -> POST /api/submissions  |
                       |  - [Playground Run] -> POST /api/compiler/run |
                       +-----------------------+-----------------------+
                                               |
                                               v
                       +-----------------------------------------------+
                       |       Spring Boot 3.2.5 REST Controllers      |
                       |  - SubmissionController.java                  |
                       |  - ProblemController.java                     |
                       |  - CompilerController.java                    |
                       +-----------------------+-----------------------+
                                               |
                                               v
                       +-----------------------------------------------+
                       |           SecurityValidator.java              |
                       |   (Blocks RCE, Sockets, Files, System.exit)   |
                       +-----------------------+-----------------------+
                                               |
                      +------------------------+------------------------+
                      |                                                 |
                      v                                                 v
       +-------------------------------+               +-------------------------------+
       |   CodeExecutionService.java   |               |  OnlineCompilerService.java   |
       |   (Testcase Judge Engine)     |               |  (Playground Scratchpad)      |
       +---------------+---------------+               +---------------+---------------+
                       |                                               |
                       +-----------------------+-----------------------+
                                               |
                                               v
       +-------------------------------------------------------------------------------+
       |                           Operating System Process Isolation                  |
       |  1. Files.createTempDirectory("oj_sandbox_")                                  |
       |  2. If Java: javac -> java -Xmx64M                                            |
       |  3. If C++:  g++ -O3 -std=c++20 -> solution.exe                               |
       |  4. Asynchronous stdout & stderr drainers                                     |
       |  5. 5.0-second watchdog enforcement (process.destroyForcibly())               |
       |  6. Cleanup: Files.walkFileTree sandbox purge                                 |
       +-------------------------------------------------------------------------------+
```

---

## 📁 Directory Structure

```
Multi cliennt chat app/
├── pom.xml                                     # Spring Boot 3.2.5, Java 21 LTS
├── mvnw.cmd                                    # Windows Maven wrapper
├── README.md                                   # Architecture documentation
├── src/
│   ├── main/
│   │   ├── java/com/judge/
│   │   │   ├── JudgeApplication.java           # Spring Boot entry point
│   │   │   ├── controller/
│   │   │   │   ├── ProblemController.java      # GET /api/problems
│   │   │   │   ├── SubmissionController.java   # POST /api/submissions/execute, /stream, /history
│   │   │   │   └── CompilerController.java     # POST /api/compiler/run, /format
│   │   │   ├── model/
│   │   │   │   ├── Problem.java                # Problem statement (Java & C++ starter codes)
│   │   │   │   ├── SubmissionRecord.java       # Past submission history model
│   │   │   │   ├── TestCase.java               # Public & hidden testcases
│   │   │   │   ├── Verdict.java                # ACCEPTED, WRONG_ANSWER, TLE, SECURITY_VIOLATION
│   │   │   │   └── ExecutionRequest.java       # Language, code, isSubmit, customInput
│   │   │   ├── repository/
│   │   │   │   ├── ProblemRepository.java      # DSA problem storage
│   │   │   │   └── SubmissionRepository.java   # History & percentile distribution engine
│   │   │   └── service/
│   │   │       ├── SecurityValidator.java      # RCE & static security analysis
│   │   │       ├── CodeExecutionService.java   # Judge test runner (Java & C++)
│   │   │       ├── OnlineCompilerService.java  # Playground execution (Java & C++)
│   │   │       └── AutoFixService.java         # Diagnostic auto-repair
│   │   └── resources/
│   │       └── static/
│   │           ├── index.html                  # LeetCode 3-pane workbench UI
│   │           ├── css/style.css               # Modern dark theme styles & resizers
│   │           └── js/app.js                   # Monaco Editor, Multi-Language & Judge runner
│   └── test/
│       └── java/com/judge/
│           └── JudgeApplicationTests.java     # 11 integration tests (Java, C++, Security)
```

---

## 🚀 Running the App

```powershell
.\mvnw.cmd spring-boot:run
```

Open your browser to: **`http://localhost:8080`**
