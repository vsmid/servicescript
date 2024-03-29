import com.sun.net.httpserver.Filter
import com.sun.net.httpserver.HttpExchange

import static ServiceScript.*
import static java.util.logging.Level.INFO

def applog = logger "CarsService"

int callCounter = 0

Map cars = [
        "Mazda" : ["Mazda 3", "Mazda 6"],
        "Suzuki": ["Swift", "Jimmy"]
]

BasicAuth basicAuth = { String username, String password -> username == "lena" && password == "123" }

Middleware requestCounter = [
        doFilter: { HttpExchange exchange, Filter.Chain chain ->
            applog.log INFO, "${exchange.requestURI} called ${++callCounter} time(s)"
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
            applog.log INFO, "Authenticated user: ${exchange.principal}"
            exchange.json 200, cars[exchange.jsondata()?.type] ?: []
        },
        middleware   : [new CORSHandler(maxAge: 2000), requestCounter],
        authenticator: basicAuth
]

Method licencePlate = [
        name      : "licencePlate",
        exchange  : { HttpExchange exchange -> exchange.out 200, "text/plain", "KR-4456-ZG".bytes },
        middleware: [requestCounter]
]

Method metrics = [
        name    : "metrics",
        exchange: { HttpExchange exchange -> exchange.json 200, ["metrics": ["calls": callCounter]] }
]

expose 1111, findAll, findOne, licencePlate, metrics