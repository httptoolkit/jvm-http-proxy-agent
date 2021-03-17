@file:JvmName("AttachMain")

package tech.httptoolkit.javaagent

import com.sun.tools.attach.*
import kotlin.system.exitProcess
import java.lang.management.ManagementFactory
import java.io.File
import java.lang.IllegalArgumentException

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
    // Self-test ensures that the JVM we're using is capable of scanning & attachment. It *doesn't* fully
    // test its ability to transform classes as we'd like.
    if (args.size == 1 && args[0] == "self-test") {
        val selfAttachAllowed = System.getProperty("jdk.attach.allowAttachSelf")
        if (selfAttachAllowed != "true") {
            throw IllegalArgumentException("Cannot run self-test without -Djdk.attach.allowAttachSelf=true")
        }

        getTargets() // Check we can scan for targets
        attachAgent(getOwnPid(), "attach-test") // Check we can attach (against ourselves)
    } else if (args.size == 1 && args[0] == "list-targets") {
        // List-targets prints a list of pid:name target paids
        val pid = getOwnPid()
        val vms = getTargets()
        vms.forEach { vmd ->
            if (vmd.id() != pid) {
                println("${vmd.id()}:${vmd.displayName()}")
            }
        }

        exitProcess(0)
    } else if (args.size == 4) {
        // 4-args format attaches to a target pid with the given config values
        val (pid, proxyHost, proxyPort, certPath) = args
        attachAgent(pid, formatConfigArg(proxyHost, proxyPort, certPath))
    } else {
        System.err.println("Usage: java -jar <agent.jar> <target-PID> <proxyHost> <proxyPort> <path-to-certificate>")
        System.err.println("Or pass a single 'self-test' or 'list-target' arg to check capabilities or scan for pids")
        exitProcess(2)
    }
}

fun getOwnPid(): String {
    // This should work in general, but it's implementation dependent:
    val pid = ManagementFactory.getRuntimeMXBean().name.split("@")[0]

    return if (pid.toLongOrNull() != null) {
        pid
    } else {
        ProcessHandle.current().pid().toString()
    }
}

fun getTargets(): List<VirtualMachineDescriptor> {
    val vms = VirtualMachine.list()
    if (vms.isEmpty()) {
        // VMs should never be empty, because at the very least _we_ should be in there! If it's empty then
        // scanning isn't working at all, and we should fail clearly.
        System.err.println("Can't scan for attachable JVMs. Are we running in a JRE instead of a JDK?")
        exitProcess(4)
    }
    return vms
}

fun attachAgent(
    pid: String,
    agentArg: String
) {
    val jarPath = File(
        ConstantProxySelector::class.java // Any arbitrary class defined inside this JAR
            .protectionDomain.codeSource.location.path
    ).absolutePath

    // Inject the agent into the target VM
    try {
        val vm: VirtualMachine = VirtualMachine.attach(pid)
        vm.loadAgent(jarPath, agentArg)
        vm.detach()
    } catch (e: AgentLoadException) {
        if (e.message == "0") {
            // This is a cross-JVM-version bug, and this is actually a success result.
            // See https://stackoverflow.com/questions/54340438/
            return
        } else {
            System.err.println("Attaching agent failed")
            e.printStackTrace()
            exitProcess(3)
        }
    }
}
