import com.sun.net.httpserver.*
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.TupleConstructor

class ServiceScript {

    private ServiceScript() {}

    static JsonSlurper JSON = new JsonSlurper()

    static abstract class BasicAuth extends BasicAuthenticator {
        BasicAuth() {
            super("basic")
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
                exchange.json 500, [error: e.message]
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

    static void expose(int port = new Random().nextInt(9000 - 5000 + 1) + 5000, Method... methods) {
        extend()

        HttpServer server
        server = HttpServer.create new InetSocketAddress(port), 0

        methods.each {
            HttpContext context = server.createContext "/" + it.name, new ExchangeHandler(it.exchange)
            if (it?.authenticator) context.setAuthenticator it.authenticator
            if (it?.middleware) context.filters.addAll it.middleware
        }

        server.setExecutor(null)

        println "[info] Available service methods:"
        println methods.collect { "/" + it.name }.join(System.lineSeparator())

        server.start()

        println "[info] Service opened on port ${port}"

        server
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
}