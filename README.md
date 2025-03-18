# jvm-http-proxy-agent

> _Part of [HTTP Toolkit](https://httptoolkit.com): powerful tools for building, testing & debugging HTTP(S)_

A JVM agent that automatically forces a proxy for HTTP(S) connections, and trusts a given additional HTTPS certificate authority, for all major JVM HTTP clients.

This agent lets you intercept all HTTP(S) from any JVM application automatically, with no code changes, so you can inspect, debug & mock this traffic using an HTTPS proxy, such as [HTTP Toolkit](https://httptoolkit.com) or any other HTTPS MitM proxy.

You can either launch the application with this agent from the start, or it can attach to and take over HTTP(S) from an already running JVM application.

Traffic can be captured from at least:

- [x] Java's built-in HttpURLConnection
- [x] Java 11's built-in HttpClient
- [x] Apache HttpClient v3, v4 & v5
- [x] Apache HttpAsyncClient v4 & v5
- [x] OkHttp v2, v3 & v4
- [x] Retrofit
- [x] Jetty-Client v9, v10 & v11
- [x] Async-Http-Client
- [x] Reactor-Netty v0.9 & v1+
- [x] Spring WebClient
- [x] Ktor-Client
- [x] Akka-HTTP v10.1.6+
- [x] Vert.x HttpClient and WebClient

This will also capture HTTP(S) from any downstream libraries based on each of these clients, and many other untested clients sharing similar implementations, and so should cover a very large percentage of HTTP client usage.

This agent supports at least Oracle & OpenJDK v8+ when starting the application with the agent, or v11+ for application that the agent will attach to.

It's likely that this supports many other HTTP client configurations & JVMs too. If you find a case that isn't supported, or isn't supported correctly, please [file an issue](https://github.com/httptoolkit/jvm-http-proxy-agent/issues/new).

## Usage

This agent can either be attached when the process is started, or attached later to a running process.

In each case, the agent must be configured with the proxy host (e.g. 127.0.0.1), the proxy port (e.g. 8000) and the absolute path to the HTTPS certificate to be trusted.

### Attaching at startup

To attach at startup, pass this JAR using the `javaagent` option, e.g:

```
java -javaagent:./agent.jar="127.0.0.1|8000|/path/to/cert.pem" -jar ./application.jar
```

### Attaching to a running process

To attach to a process, first launch the target process, and then run:

```
java -jar ./agent.jar 1234 127.0.0.1 8000 /path/to/cert.pem
```

where 1234 is the pid of the target process. This will exit successfully & immediately if attachment succeeds, or with a non-zero exit code if not.

You can also query the available JVM processes ids, like so:

```
> java -jar ./agent.jar list-targets
589739:./application.jar
404401:com.intellij.idea.Main
453889:org.jetbrains.kotlin.daemon.KotlinCompileDaemon --daemon-runFilesPath ...
413868:org.gradle.launcher.daemon.bootstrap.GradleDaemon 6.7
```

When attached from startup all clients will always be intercepted. When attached later, both newly created HTTP clients and already existing instances will be intercepted, but it's possible that in some cases already established connections may not be immediately captured. Typically though these will eventually close and be reconnected, and at that point the connection is always intercepted.

### Testing attachment capabilities

Not all JDKs provide the instrumentation & attachment APIs required to support this process, although all Oracle & OpenJDK v9+ versions should do so.

To check this, you can test whether the `java` in your $PATH is capable of attaching to and intercepting a target process using the self-test command, like so:

```bash
java -Djdk.attach.allowAttachSelf=true -jar ./agent.jar self-test
```

### Contributing

Contributions are very welcome! Reports of scenarios that aren't currently supported are helpful (please [create an issue](https://github.com/httptoolkit/jvm-http-proxy-agent/issues/new), including any errors, and preferably steps to reproduce the issue) but patches to fix issues are even better.

Be aware that for all contributors to HTTP Toolkit components, including this, [HTTP Toolkit Pro is totally free](https://github.com/httptoolkit/httptoolkit/#contributing-directly) - just [get in touch](https://httptoolkit.com/contact) after your contribution is accepted with the email you'd like to use to claim your Pro account.

To contribute a patch:

* Fork this repo
* Clone your fork: `git clone git@github.com:$YOUR_GITHUB_USERNAME/jvm-http-proxy-agent.git`
* Create a new branch: `git checkout -B my-contribution-branch`
* Check the existing tests pass locally: `./gradlew quickTest`
  * N.B. this requires Java 11+ - while some features are supported in older JVM versions, development requires a modern JVM
* For library-specific issues:
  * Add/edit a test case in [/test-app/src/main/java/tech/httptoolkit/testapp/cases](https://github.com/httptoolkit/jvm-http-proxy-agent/tree/main/test-app/src/main/java/tech/httptoolkit/testapp/cases) to reproduce your issue
  * Add that case to [the list](https://github.com/httptoolkit/jvm-http-proxy-agent/blob/459b931a2eebd486261f296418aa028e4b2fb7e9/test-app/src/main/java/tech/httptoolkit/testapp/Main.java#L17-L36) if you created a new case.
  * Check that `./gradlew quickTest` now fails.
* For more general changes:
  * Either add a test case (as above) or add a new standalone test in https://github.com/httptoolkit/jvm-http-proxy-agent/blob/main/src/test/kotlin/IntegrationTests.kt
* Make your changes within the [advice classes](https://github.com/httptoolkit/jvm-http-proxy-agent/tree/main/src/main/java/tech/httptoolkit/javaagent/advice) and [injection setup code](https://github.com/httptoolkit/jvm-http-proxy-agent/tree/main/src/main/kotlin/tech/httptoolkit/javaagent) to fix your issue/add your feature.
* Test that `./gradlew quickTest` now passes.
* If you've changed any functionality, consider adding it to the docs here.
* Commit your change, push it, and open a PR here for review.

If you have any issues, or if you want to discuss a change before working on it (recommended for large/complex changes), please [open an issue](https://github.com/httptoolkit/jvm-http-proxy-agent/issues/new).
