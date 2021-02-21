@file:JvmName("AttachMain")

package tech.httptoolkit.javaagent

import com.sun.tools.attach.*
import kotlin.system.exitProcess
import java.lang.management.ManagementFactory
import java.io.File

// This file is the only one that uses com.sun.tools.attach.VirtualMachine. That's important because that
// requires tools.jar to be in your classpath (i.e. it requires a JDK not a JRE). If you run this without
// a JDK, you'll get errors about that class being unavailable.

// Check the class is available before we properly use it - the goal being that this fails clearly before
// actually using VirtualMachine fails confusingly.
val x: Class<*> = try { // Var declaration required for static init for some reason
    Class.forName("com.sun.tools.attach.VirtualMachine")
} catch (e: ClassNotFoundException) {
    System.err.println(
        "Could not start. Attaching to a running virtual machine requires a JDK including com.sun.tools.attach, not a JRE."
    )
    exitProcess(1)
}

// If run directly, can either list potential targets (list-targets) or attach to a target (pid, ...config)
fun main(args: Array<String>) {
    if (args.size == 1 && args[0] == "list-targets") {
        // This isn't guaranteed to work everywhere, but it should work in most places:
        val (pid) = ManagementFactory.getRuntimeMXBean().name.split("@")


        VirtualMachine.list().forEach { vmd ->
            if (vmd.id() != pid) {
                println("${vmd.id()}:${vmd.displayName()}")
            }
        }
        exitProcess(0)
    } else if (args.size != 4) {
        System.err.println("Usage: java -jar <agent.jar> <target-PID> <proxyHost> <proxyPort> <path-to-certificate>")
        exitProcess(2)
    }

    val (pid, proxyHost, proxyPort, certPath) = args

    val jarPath = File(
        ConstantProxySelector::class.java // Any arbitrary class defined inside this JAR
            .protectionDomain.codeSource.location.path
    ).absolutePath

    // Inject the agent with our config arguments into the target VM
    val vm: VirtualMachine = VirtualMachine.attach(pid)
    vm.loadAgent(jarPath, formatConfigArg(proxyHost, proxyPort, certPath))
    vm.detach()
}