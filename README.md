# jvm-http-proxy-agent

> _Part of [HTTP Toolkit](https://httptoolkit.tech): powerful tools for building, testing & debugging HTTP(S)_

A JVM agent that automatically forces a proxy for HTTP(S) connections, and trusts a given additional HTTPS certificate authority, for all major JVM HTTP clients.

This agent lets you intercept all HTTP(S) from any JVM application automatically, with no code changes, so you can inspect, debug & mock this traffic using an HTTPS proxy, such as [HTTP Toolkit](https://httptoolkit.tech) or any other HTTPS MitM proxy.

You can either launch the application with this agent from the start, or it can attach to and take over HTTP(S) from an already running JVM application.

Traffic can be captured from at least:

- [x] Java's built-in HttpURLConnection
- [x] Java 11's built-in HttpClient
- [x] Apache HttpClient v4 & v5
- [x] OkHttp v2, v3 & v4
- [x] Retrofit
- [x] Jetty-Client v9, 10 & 11
- [x] Async-Http-Client

This will also capture HTTP(S) from any downstream libraries based on each of these clients, and many other untested clients sharing similar implementations, and so should cover a very large percentage of HTTP client usage.

This agent supports at least Oracle & OpenJDK v8+ when starting the application with the agent, or v11+ for application that the agent will attach to.

It's likely that this supports many other HTTP client configurations & JVMs too. If you find a case that isn't supported, or isn't supported correctly, please [file an issue](https://github.com/httptoolkit/jvm-http-proxy-agent/issues/new).

## Usage

This agent can either be attached when the process is started, or attached later to a running process.

In each case, the agent must be configured with the proxy host (e.g. 127.0.0.1), the proxy port (e.g. 8000) and the absolute path to the HTTPS certificate to be trusted.

To attach at startup, pass this JAR using the `javaagent` option, e.g:

```
java -javaagent:./agent.jar=127.0.0.1|8000|/path/to/cert.pem -jar ./application.jar
```

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