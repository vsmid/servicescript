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

```groovy
// Basic example
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
## Example
See [CarsService.groovy](./CarsService.groovy).
