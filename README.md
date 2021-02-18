# jvm-http-proxy-agent

> _Part of [HTTP Toolkit](https://httptoolkit.tech): powerful tools for building, testing & debugging HTTP(S)_

A JVM agent that automatically forces a proxy for HTTP(S) connections, and optionally trusts MitM certificates, for all major JVM HTTP clients.

This is designed to power JVM interception within HTTP Toolkit, by allowing automatic MitM proxy interception of existing applications. Designed to be functional either when passed on the command line, or attached to a running JVM instance.

This agent injects both proxy configuration and ensures trust of a CA certificate, allowing inspection & rewriting of all outbound HTTPS traffic.

This agent can capture traffic from:

- [x] Java's built-in HttpURLConnection
- [x] Java 11's built-in HttpClient
- [ ] Apache HttpClient v4
- [ ] Apache HttpClient v5
- [x] OkHttp v2
- [x] OkHttp v3
- [x] OkHttp v4
- [x] Retrofit

In general, this will also capture HTTP(S) from any downstream libraries based on each of these clients, and so covers a very large area of HTTP usage.

If you have some code that isn't correctly intercepted, please [file an issue](https://github.com/httptoolkit/jvm-http-proxy-agent/issues/new). 