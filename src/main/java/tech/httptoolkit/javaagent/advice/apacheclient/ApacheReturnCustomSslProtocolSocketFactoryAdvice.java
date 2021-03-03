package tech.httptoolkit.javaagent.advice.apacheclient;

import net.bytebuddy.asm.Advice;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

public class ApacheReturnCustomSslProtocolSocketFactoryAdvice {
    
    @Advice.OnMethodExit
    public static void getSocketFactory(
        @Advice.FieldValue(value = "secure") boolean isSecure,
        @Advice.Return(readOnly = false) ProtocolSocketFactory returnValue
    ) {
        if (isSecure) {
            returnValue = new ApacheCustomSslProtocolSocketFactory();
        }
    }
}
