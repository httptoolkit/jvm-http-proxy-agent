package tech.httptoolkit.javaagent.advice;

import net.bytebuddy.asm.Advice;

// General purpose advice which skips a given method, returning the default value for its type
// (so usually null) if there is a return value, and silently doing nothing.
public class SkipMethodAdvice {
    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class) // => skip if we return true (or similar)
    public static boolean skipMethod() {
        return true; // Skip the method body entirely
    }
}
