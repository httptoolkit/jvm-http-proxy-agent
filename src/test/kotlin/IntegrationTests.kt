@file:Suppress("BlockingMethodInNonBlockingContext")

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.shouldBe
import java.lang.Thread.sleep
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

val resourcesPath = Paths.get("src", "test", "resources")!!

const val proxyHost = "127.0.0.1"
val certPath = resourcesPath.resolve("cert.pem").toAbsolutePath().toString()

// We always launch subprocesses with the same Java that we're using ourselves
val javaPath = Paths.get(System.getProperty("java.home"), "bin", "java").toString()

val wireMockServer = WireMockServer(options()
    .dynamicPort()
    .enableBrowserProxying(true)
    .caKeystorePath(resourcesPath.resolve("cert.jks").toAbsolutePath().toString())
    .caKeystorePassword("password")
)

val runningProcs = arrayListOf<Process>()

class IntegrationTests : StringSpec({
    "Launching with -javaagent should intercept all clients" {

        val agentArgs = "$proxyHost|${wireMockServer.port()}|$certPath"

        val proc = ProcessBuilder(
            javaPath,
            "-javaagent:./build/libs/http-proxy-agent-1.0-SNAPSHOT-all.jar=$agentArgs",
            "-jar", "./test-app/build/libs/test-app-1.0-SNAPSHOT-all.jar"
        ).inheritIO().start()
        runningProcs.add(proc)

        proc.waitFor(10, TimeUnit.SECONDS)

        proc.isAlive.shouldBe(false)
        proc.exitValue().shouldBe(0)
    }

    "Launching directly and attaching later should eventually intercept all clients" {
        // Start up the target:
        val targetProc = ProcessBuilder(
            javaPath,
            "-jar", "./test-app/build/libs/test-app-1.0-SNAPSHOT-all.jar"
        ).inheritIO().start()
        runningProcs.add(targetProc)

        targetProc.inputStream.read() // Wait until some output appears
        sleep(2000) // Sleep a little extra, to check everything's fully running

        // It shouldn't quit yet - it should block until we intercept
        targetProc.isAlive.shouldBe(true)

        // Attach the agent:
        val agentAttachProc = ProcessBuilder(
            javaPath,
            "-jar", "./build/libs/http-proxy-agent-1.0-SNAPSHOT-all.jar",
            targetProc.pid().toString(), proxyHost, wireMockServer.port().toString(), certPath
        ).inheritIO().start()
        runningProcs.add(agentAttachProc)

        // Agent attacher should quit happily
        agentAttachProc.waitFor(2, TimeUnit.SECONDS)
        agentAttachProc.isAlive.shouldBe(false)
        agentAttachProc.exitValue().shouldBe(0)

        // Target should pick up proxy details & quit happily too
        targetProc.waitFor(10, TimeUnit.SECONDS)
        targetProc.isAlive.shouldBe(false)
        targetProc.exitValue().shouldBe(0)
    }
}) {
    override fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)

        runningProcs.clear()
        wireMockServer.start()

        // Send a 200 response for all requests
        wireMockServer.stubFor(any(anyUrl()).willReturn(ok()))
    }

    override fun afterTest(testCase: TestCase, result: TestResult) {
        super.afterTest(testCase, result)
        wireMockServer.stop()
        runningProcs.forEach { proc -> proc.destroyForcibly() }
    }
}