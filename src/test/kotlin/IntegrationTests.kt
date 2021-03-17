@file:Suppress("BlockingMethodInNonBlockingContext")

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import java.io.BufferedReader
import java.lang.Thread.sleep
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

// We require TEST_JAR to always be set when running the tests, giving us the appropriate path
// to the specific agent jar that we're testing.
val AGENT_JAR_PATH = System.getenv("TEST_JAR")!!
val x = run {
    println("Testing $AGENT_JAR_PATH")
}

// For the test-app JAR we just use a constant string
val TEST_APP_JAR = Paths.get("test-app", "build", "libs", "test-app-1.0-SNAPSHOT-all.jar").toString()

val resourcesPath: Path = Paths.get("src", "test", "resources")

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
    "Launching a self test should return successfully" {
        val proc = ProcessBuilder(
            javaPath,
            "-Djdk.attach.allowAttachSelf=true",
            "-jar", AGENT_JAR_PATH,
            "self-test"
        ).start()
        runningProcs.add(proc)

        proc.waitFor(10, TimeUnit.SECONDS)

        proc.isAlive.shouldBe(false)
        proc.exitValue().shouldBe(0)
    }

    "Launching with list-targets should return successfully" {
        val proc = ProcessBuilder(
            javaPath,
            "-jar", AGENT_JAR_PATH,
            "list-targets"
        ).start()
        runningProcs.add(proc)
        val outputReader = proc.inputStream.bufferedReader()

        proc.waitFor(10, TimeUnit.SECONDS)

        val output = outputReader.use(BufferedReader::readText)
        output.shouldNotBeEmpty()

        proc.isAlive.shouldBe(false)
        proc.exitValue().shouldBe(0)
    }

    "Launching with -javaagent should intercept all clients" {

        val agentArgs = "$proxyHost|${wireMockServer.port()}|$certPath"

        val proc = ProcessBuilder(
            javaPath,
            "-javaagent:$AGENT_JAR_PATH=$agentArgs",
            "-jar", TEST_APP_JAR
        ).inheritIO().start()
        runningProcs.add(proc)

        proc.waitFor(30, TimeUnit.SECONDS)

        proc.isAlive.shouldBe(false)
        proc.exitValue().shouldBe(0)
    }

    "Launching directly and attaching later should eventually intercept all clients" {
        // Start up the target:
        val targetProc = ProcessBuilder(
            javaPath,
            "-jar", TEST_APP_JAR
        ).inheritIO().start()
        runningProcs.add(targetProc)

        targetProc.inputStream.read() // Wait until some output appears
        sleep(2000) // Sleep a little extra, to check everything's fully running

        // It shouldn't quit yet - it should block until we intercept
        targetProc.isAlive.shouldBe(true)

        // Attach the agent:
        val agentAttachProc = ProcessBuilder(
            javaPath,
            "-jar", AGENT_JAR_PATH,
            targetProc.pid().toString(), proxyHost, wireMockServer.port().toString(), certPath
        ).inheritIO().start()
        runningProcs.add(agentAttachProc)

        // Agent attacher should quit happily
        agentAttachProc.waitFor(30, TimeUnit.SECONDS)
        agentAttachProc.isAlive.shouldBe(false)
        agentAttachProc.exitValue().shouldBe(0)

        // Target should pick up proxy details & quit happily, eventually
        targetProc.waitFor(30, TimeUnit.SECONDS)
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