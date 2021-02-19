package tech.httptoolkit.javaagent

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.matcher.ElementMatchers.*
import net.bytebuddy.utility.JavaModule
import javax.net.ssl.SSLContext
import java.lang.IllegalStateException

// For both v4 & v5 we override all implementations of the RoutePlanner interface, and we redefine all routes
// to go via our proxy instead of their existing configuration.

class ApacheClientRoutingV4Transformer(
    private val proxyHost: String,
    private val proxyPort: Int
) : MatchingAgentTransformer {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder.type(
            hasSuperType(named("org.apache.http.conn.routing.HttpRoutePlanner"))
        ).and(
            not(isInterface())
        ).transform(this)
    }

    override fun transform(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classLoader: ClassLoader?,
        module: JavaModule?
    ): DynamicType.Builder<*>? {
        val proxyHost = org.apache.http.HttpHost(proxyHost, proxyPort)

        return builder
            .method(named("determineRoute"))
            .intercept(MethodDelegation.to(HttpRouteV4Interceptor(proxyHost)))
    }
}

class HttpRouteV4Interceptor(
    private val proxyHost: org.apache.http.HttpHost
) {
    fun determineRoute(
        host: org.apache.http.HttpHost,
        request: org.apache.http.HttpRequest,
        context: org.apache.http.protocol.HttpContext
    ): org.apache.http.conn.routing.HttpRoute {
        // Implementation translated from DefaultRoutePlanner, stripped down
        // with proxy routing enforced.
        val clientContext = org.apache.http.client.protocol.HttpClientContext.adapt(context)
        val config = clientContext.requestConfig
        val local = config.localAddress

        val target = if (host.port <= 0) {
            org.apache.http.HttpHost(
                host.hostName,
                org.apache.http.impl.conn.DefaultSchemePortResolver
                    .INSTANCE.resolve(host),
                host.schemeName
            )
        } else {
            host
        }

        val secure = target.schemeName.equals("https", ignoreCase = true)
        return org.apache.http.conn.routing.HttpRoute(target, local, proxyHost, secure)
    }
}
class ApacheClientRoutingV5Transformer(
    private val proxyHost: String,
    private val proxyPort: Int
) : MatchingAgentTransformer {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder.type(
            hasSuperType(named("org.apache.hc.client5.http.routing.HttpRoutePlanner"))
        ).and(
            not(isInterface())
        ).transform(this)
    }

    override fun transform(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classLoader: ClassLoader?,
        module: JavaModule?
    ): DynamicType.Builder<*>? {
        val proxyHost = org.apache.hc.core5.http.HttpHost(proxyHost, proxyPort)

        return builder
            .method(named("determineRoute"))
            .intercept(MethodDelegation.to(HttpRouteV5Interceptor(proxyHost)))
    }
}

class HttpRouteV5Interceptor(
    private val proxyHost: org.apache.hc.core5.http.HttpHost
) {
    fun determineRoute(
        host: org.apache.hc.core5.http.HttpHost,
        context: org.apache.hc.core5.http.protocol.HttpContext
    ): org.apache.hc.client5.http.HttpRoute {
        // Implementation translated from DefaultRoutePlanner, stripped down
        // with proxy routing enforced.
        val schemePortResolver = org.apache.hc.client5.http.impl.DefaultSchemePortResolver.INSTANCE
        val target = org.apache.hc.client5.http.routing.RoutingSupport.normalize(host, schemePortResolver)
        if (target.port < 0) {
            throw org.apache.hc.core5.http.ProtocolException("Unroutable protocol scheme: $target")
        }
        val secure: Boolean = target.schemeName.equals("https", ignoreCase = true)
        return org.apache.hc.client5.http.HttpRoute(target, null, proxyHost, secure)
    }
}

// For certificates, we append to Apache SslConnectionSocketFactory's constructors using visitors (can't intercept &
// call super otherwise) to replace the configured SslSocketFactory with one of our own that uses our configured
// SSLContext, which trusts our certificate, straight after initialization.

class ApacheSslSocketFactoryTransformer : MatchingAgentTransformer {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                named("org.apache.http.conn.ssl.SSLConnectionSocketFactory")
            ).transform(this)
            .type(
                named("org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory")
            ).transform(this)
    }

    override fun transform(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classLoader: ClassLoader?,
        module: JavaModule?
    ): DynamicType.Builder<*>? {
        return builder.visit(
            Advice.to(SslSocketFactoryInterceptor::class.java).on(isConstructor())
        );
    }
}

// Two possible fields: the first used by v4, the second used by recent versions of v5. Rather than carefully
// work out which is present, we just look for both all the time
private val SslFactoryFields = arrayOf("socketfactory", "socketFactory")

object SslSocketFactoryInterceptor {
    @Advice.OnMethodExit
    @JvmStatic
    fun afterConstructor(
        @Advice.This thisFactory: Any
    ) {
        // Detect which field(s) are present on this class
        val socketFactoryFields = SslFactoryFields.filter { factoryFieldName ->
            try {
                thisFactory.javaClass.getDeclaredField(factoryFieldName)
                true
            } catch (e: NoSuchFieldException) {
                false
            }
        }

        if (socketFactoryFields.isEmpty()) {
            throw IllegalStateException("Apache SSLConnectionSocketFactory has no available socketFactory field")
        }

        socketFactoryFields.forEach { factoryFieldName ->
            val socketFactoryField = thisFactory.javaClass.getDeclaredField(factoryFieldName)
            // Allow ourselves to change the socket value
            socketFactoryField.isAccessible = true

            // Overwrite the socket factory with our own:
            socketFactoryField.set(thisFactory, interceptedSslContext.socketFactory)
        }
    }
}