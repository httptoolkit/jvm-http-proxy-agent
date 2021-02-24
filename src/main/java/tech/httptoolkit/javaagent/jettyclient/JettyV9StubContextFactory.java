package tech.httptoolkit.javaagent.jettyclient;

import org.eclipse.jetty.util.ssl.SslContextFactory;

// Does absolutely nothing, except for not being abstract in the v10 types, which means we can
// properly instantiate it in v9. This is part of the silly games required to support both
// versions whilst we can only depend directly on one at compile time.
public class JettyV9StubContextFactory extends SslContextFactory {
}
