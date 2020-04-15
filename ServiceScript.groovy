import com.sun.net.httpserver.*
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.TupleConstructor

import java.util.concurrent.Executors
import java.util.function.Function

import static java.lang.System.Logger.Level.ERROR
import static java.lang.System.Logger.Level.INFO

class ServiceScript {

    private static System.Logger syslog = System.getLogger ServiceScript.name

    static JsonSlurper JSON = new JsonSlurper()
    static HttpServer HTTP_SERVER

    static {
        System.setProperty "java.util.logging.SimpleFormatter.format", "[%1\$tF %1\$tT %1\$tL] [%4\$-1s] [%3\$s] %5\$s %n"
        extend()
    }

    private ServiceScript() {}

    static System.Logger logger(String name) {
        System.getLogger name
    }

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

        methods.each { syslog.log INFO, "Registered method: /${it.name}" }

        HTTP_SERVER.start()

        syslog.log INFO, "Service is listening on port ${HTTP_SERVER.address.port}"
    }

    static void extend() {
        HttpExchange.metaClass {
            out { status = 200, contentType = "*/*", content ->
                delegate.responseHeaders.add "Content-type", contentType
                delegate.sendResponseHeaders status, content?.length ?: -1
                if (content) delegate.responseBody.write content
                delegate.close()
            }
            json { status = 200, content -> delegate.out status, "application/json", JsonOutput.toJson(content).bytes }
            streamout { status = 200, contentType = "application/octet-stream", is ->
                delegate.responseHeaders.add "Content-type", contentType
                delegate.sendResponseHeaders status, 0
                delegate.responseBody.withStream {
                    out -> is.withStream { it.eachByte { out.write(it) } }
                }
                delegate.close()
            }
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
                syslog.log ERROR, "${e.message}"
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