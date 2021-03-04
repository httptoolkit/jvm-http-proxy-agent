package tech.httptoolkit.testapp.cases

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking

class KtorCioCase : ClientCase<HttpClient>() {

    override fun newClient(url: String): HttpClient {
        return HttpClient(CIO)
    }

    override fun stopClient(client: HttpClient) {
        client.close()
    }

    override fun test(url: String, client: HttpClient): Int = runBlocking {
        val response = client.get<HttpResponse>(url)
        response.status.value
    }

}