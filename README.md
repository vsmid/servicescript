# ServiceScript

## Why?
Highly opinionated, zero dependency Groovy script powered fast http service prototyping without creating a project.
This is not designed with REST in mind.

## Prerequisites
* Java
* Groovy

## Some details
This script is basically a wrapper around Java's [HttpServer](https://docs.oracle.com/en/java/javase/11/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpServer.html). There is no magic, it works just like described in the docs.
It adds some convenient methods to `com.sun.net.httpserver.HttpExchange` class for easier manipulation of incoming request and outgoing response.

## How to use it?
Just download [ServiceScript.groovy](./ServiceScript.groovy) file to you local machine.
You can now import that file to any other Groovy script you want using classic `import` statement.

## Method, Middleware and Auth classes
**Method**

Describe and implement operation per single request. Following properties can be set:
* `name` - single word to name the operation, serves as URI. Do not put slashes or regex, just enter unique, simple name.       
* `exchange` - implement operation. Closure taking `com.sun.net.httpserver.HttpExchange` as single parameter.       
* `middleware` - a lit of middlewares(classes which are of type `ServiceScript.Middleware` or extend  `com.sun.net.httpserver.Filter`) executed in order.       
* `authenticator` - how is user authenticated. You can use any implementation of `com.sun.net.httpserver.Authenticator`. ServiceScript provides `BasicAuth` class which you can use instead `com.sun.net.httpserver.BasicAuthenticator` but either is fine. See `BasicAuth` usage in below given examples.

**Middleware**

Same as `com.sun.net.httpserver.Filter` but more convenient for Groovy instantiation..

**Auth**

Same as Java's `com.sun.net.httpserver.Authenticator` but more convenient for Groovy instantiation. This one you can put under Method's `authenticator` property.

#### One-liner example
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

// BasicAuth. 
// You can add a custom one by using ServiceScript's Auth or by implementing com.sun.net.httpserver.Authenticator.

// Auth example:
/*Auth customAuth = [
    authenticate: { HttpExchange exchange ->
      new Authenticator.Success(new HttpPrincipal("lena", "basic"))
    }
]*/

// Authenticator example:
/*Authenticator authenticator = new Authenticator() {
  @Override
  Authenticator.Result authenticate(HttpExchange exch) {
    new Authenticator.Failure(401)
  }
}*/

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
* `HttpExchange#streamout(int status, String contentType, InputStream content)` - write response as a stream. e.g. download documents etc.
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

## Exception handling during exchange
You can use classic try/catch to add customized exception handling.
If you do not provide any ServiceScript will report all unhandled exceptions by responding with `http status 500` and by setting `Reason` http header with exception message. 

## Providing custom HttpServer
In case you are not satisfied with default http server or you would like to create https server you can provide your own on the fly.
```groovy
import com.sun.net.httpserver.HttpServer
import static ServiceScript.*

HTTP_SERVER = HttpServer.create new InetSocketAddress(8888), 15
...
```

## Logging
Java's `System.Logger` is used for logging inside ServiceSrcipt.
`ServiceScript#logger(String name)` method which returns `System.Logger` is provided and you can use it in your scripts for logging.

## Full example
See [CarsService.groovy](./CarsService.groovy).
