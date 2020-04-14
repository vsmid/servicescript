import com.sun.net.httpserver.Filter
import com.sun.net.httpserver.HttpExchange

import static ServiceScript.*

int callCounter = 0

Map cars = [
        "Mazda" : ["Mazda 3", "Mazda 6"],
        "Suzuki": ["Swift", "Jimmy"]
]

BasicAuth basicAuth = { String username, String password -> username == "lena" && password == "123" }

Middleware requestCounter = [
        doFilter: { HttpExchange exchange, Filter.Chain chain ->
            println "${exchange.requestURI} called ${++callCounter} time(s)"
            chain.doFilter exchange
        }
]

Method findAll = [
        name      : "findAll",
        exchange  : { HttpExchange exchange -> exchange.json 200, cars },
        middleware: [requestCounter]
]

Method findOne = [
        name         : "findOne",
        exchange     : { HttpExchange exchange ->
            println "Authenticated user: ${exchange.principal}"
            exchange.json 200, cars[exchange.jsondata()?.type] ?: []
        },
        middleware   : [requestCounter],
        authenticator: basicAuth
]

Method metrics = [
        name    : "metrics",
        exchange: { HttpExchange exchange -> exchange.json 200, ["metrics": ["calls": callCounter]] }
]

expose 1111, findAll, findOne, metrics