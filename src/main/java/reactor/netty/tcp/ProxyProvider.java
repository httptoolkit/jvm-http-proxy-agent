package reactor.netty.tcp;

import java.net.InetSocketAddress;

/**
 * A stub with the parts of the interface we need to support v0.9 of Reactor-Netty. We compile against this, but we
 * *don't* include this in the resulting JAR, so references instead resolve to the real implementation, when that
 * is present. This is required because we can't depend on both v0.9 and v1 in the same module, and this class has
 * moved packages between the two.
 */
public final class ProxyProvider {

    public static ProxyProvider.TypeSpec builder() {
        return new ProxyProvider.Build();
    }

    ProxyProvider(ProxyProvider.Build builder) {}

    public enum Proxy {
        HTTP
    }

    static final class Build implements TypeSpec, AddressSpec, Builder {

        Build() {}

        public final Builder address(InetSocketAddress address) {
            return this;
        }

        public final AddressSpec type(Proxy type) {
            return this;
        }

        public ProxyProvider build() {
            return new ProxyProvider(this);
        }
    }

    public interface TypeSpec {
        AddressSpec type(Proxy type);
    }

    public interface AddressSpec {
        Builder address(InetSocketAddress address);
    }

    public interface Builder {
        ProxyProvider build();
    }
}
