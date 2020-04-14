# ServiceScript

## Why?
Highly opinionated, zero dependency Groovy script powered fast http service prototyping without creating a project.
This is not designed with REST in mind.

## Prerequisites
* Java
* Groovy

## Some details
This script based around Java's HttpServer class.
It adds some convenient methods to HttpExchange class for easier manipulation of incoming request and outgoing response.

## How to use it?
Just download [ServiceScript.groovy](./ServiceScript.groovy) file to you local machine.
You can now import that file to any other Groovy script you want using classic `import` statement.

#### One liner example
```groovy
import com.sun.net.httpserver.Filter
import com.sun.net.httpserver.HttpExchange
import static ServiceScript.*

// curl localhost:6666/greet
expose 6666, [name: "greet", exchange: { HttpExchange exchange -> exchange.out 200, "text/plain", "Hi".bytes }] as Method
```

#### Basic example
```groovy
import com.sun.net.httpserver.Filter
import com.sun.net.httpserver.HttpExchange
import static ServiceScript.*

Map cars = [
        "Mazda" : ["Mazda 3", "Mazda 6"],
        "Suzuki": ["Swift", "Jimmy"]
]

Method findAll = [
      name      : "findAll",
      exchange  : { HttpExchange exchange -> exchange.json 200, cars }
]

// curl localhost:6666/findAll
expose 6666, findAll
```

####  Middleware and authentication example
```groovy
import com.sun.net.httpserver.Filter
import com.sun.net.httpserver.HttpExchange
import static ServiceScript.*

int callCounter = 0;

// BasicAuth. You can add a cusotm one by implementing com.sun.net.httpserver.Authenticator.
BasicAuth basicAuth = { String username, String password -> username == "lena" && password == "123" }

Middleware requestCounter = [
        doFilter: { HttpExchange exchange, Filter.Chain chain ->
            println "${exchange.requestURI} called ${++callCounter} time(s)"
            chain.doFilter exchange
        }
]
        
Method secured = [
        name         : "secured",
        exchange     : { HttpExchange exchange ->
            println "Authenticated user: ${exchange.principal}"
            exchange.json 200, [success: true"]
        },
        middleware   : [requestCounter], // Add as many filters as you like. Executed in order.
        authenticator: basicAuth 
]

// curl -u "lena:123" localhost:6666/secured
expose 6666, secured
```

## Request/Response handling
Everything revolves around HttpExchange object during exchange phase.
Here is a list of added helper methods you can use:

* `HttpExchange#out(int status, String contentType, byte[] content)` - call to write and commit response
* `HttpExchange#json(int status, Object content)` - wrapper around out method for fast json response
* `HttpExchange#textdata` - call to get request body as text
* `HttpExchange#jsondata` - call to get request body as json

```groovy
Method someMethod = [
        name         : "secured",
        exchange     : { HttpExchange exchange ->
            String data = exchange.textdata() // or exchange.jsondata() to get data as json
            exchange.json 200, [success: true] // or exchange.out 200, "application/json", Jsonoutput.toJson([success:true]).bytes
        } 
]
```

## Providing custom HttpServer
In case you are not satisfied with default http server or you would like to create https server you can provide your own on the fly.
```groovy
import com.sun.net.httpserver.HttpServer
import static ServiceScript.*

HTTP_SERVER = HttpServer.create new InetSocketAddress(8888), 15
...
```

## Full example
See [CarsService.groovy](./CarsService.groovy).
