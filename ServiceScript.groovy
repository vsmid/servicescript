import com.sun.net.httpserver.*
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.TupleConstructor

import java.util.concurrent.Executors
import java.util.function.Function

class ServiceScript {

    static JsonSlurper JSON = new JsonSlurper()
    static HttpServer HTTP_SERVER

    static {
        extend()
    }

    private ServiceScript() {}

    static void expose(int port = new Random().nextInt(9000 - 5000 + 1) + 5000, Method... methods) {
        if (!HTTP_SERVER) {
            HTTP_SERVER = HttpServer.create new InetSocketAddress(port), 10
            HTTP_SERVER.setExecutor Executors.newFixedThreadPool(25)
        }

        methods.each {
            HttpContext context = HTTP_SERVER.createContext "/" + it.name, new ExchangeHandler(it.exchange)
            if (it?.authenticator) context.setAuthenticator it.authenticator
            if (it?.middleware) context.filters.addAll it.middleware
        }

        println "[info] Available service methods:"
        println methods.collect { "/" + it.name }.join(System.lineSeparator())

        HTTP_SERVER.start()

        println "[info] Service opened on port ${HTTP_SERVER.address.port}"
    }

    static void extend() {
        HttpExchange.metaClass {
            out { status = 200, contentType = "*/*", content = "".bytes ->
                delegate.responseHeaders.add "Content-type", contentType
                delegate.sendResponseHeaders status, content.length
                delegate.responseBody.write content
                delegate.close()
            }
            json { status = 200, content -> delegate.out status, "application/json", JsonOutput.toJson(content).bytes }
            jsondata { JSON.parse delegate.requestBody }
            textdata { new String(delegate.requestBody.readAllBytes()) }
        }
    }

    static class Method {
        String name
        Closure exchange
        List<Middleware> middleware
        Authenticator authenticator
    }

    @TupleConstructor
    static class ExchangeHandler implements HttpHandler {
        Closure handler

        @Override
        void handle(HttpExchange exchange) throws IOException {
            try {
                this.handler.call exchange
            } catch (e) {
                println "[error] ${e.message}"
                exchange.responseHeaders.add "Reason", e.message
                exchange.out 500, "*/*", "".bytes
            }
        }
    }

    static class Middleware extends Filter {
        Closure doFilter

        @Override
        void doFilter(HttpExchange exchange, Chain chain) throws IOException {
            this.doFilter.call exchange, chain
        }

        @Override
        String description() {
            ""
        }
    }

    static class Auth extends Authenticator {
        Function<HttpExchange, Result> authenticate

        @Override
        Result authenticate(HttpExchange exchange) {
           authenticate.apply exchange
        }
    }

    static abstract class BasicAuth extends BasicAuthenticator {
        BasicAuth() {
            super("basic")
        }
    }
}