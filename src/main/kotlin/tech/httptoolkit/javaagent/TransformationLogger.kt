package tech.httptoolkit.javaagent

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.utility.JavaModule

class TransformationLogger(private val debugMode: Boolean) : AgentBuilder.Listener.Adapter() {

    private val transformingTypes: ArrayList<String> = ArrayList()

    fun beforeTransformation(type: TypeDescription) {
        transformingTypes.add(type.canonicalName)
    }

    override fun onError(
        typeName: String,
        classLoader: ClassLoader?,
        module: JavaModule?,
        loaded: Boolean,
        throwable: Throwable
    ) {
        when {
            transformingTypes.contains(typeName) -> {
                System.err.println("Error configuring proxy hooks for $typeName:")
                throwable.printStackTrace(System.err)
            }
            debugMode -> {
                System.err.println("Unexpected agent configuration error $typeName:")
                throwable.printStackTrace(System.err)
            }
        }
    }

    override fun onTransformation(
        typeDescription: TypeDescription?,
        classLoader: ClassLoader?,
        module: JavaModule?,
        loaded: Boolean,
        dynamicType: DynamicType?
    ) {
        if (debugMode) {
            println("Proxy hooks configured for $typeDescription")
        }
    }
}