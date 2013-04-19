## Assumptions

- JDK1.7 required.
- Vert.x is installed and its bin directory has been added to the PATH environment variable.Plase refer:<http://vertx.io/install.html>.
- MongoDB is installed.

## Run Application

- Start mongodb on default port

- Go to $WORK_SPACE/vertx-pubsub and run command:

    $ mvn clean compile

- Run command to start server:

    $ vertx run me.timtang.server.VertxFeedApplication -cp ./target/classes -repo vert-x.github.io

> Server will start at port 8080, visit <http://localhost:8080>

## Load Testing

- Run following command to start server:

    $ vertx run me.timtang.benchmark.VertxFeedBenchmark -cp ./target/classes -repo vert-x.github.io

- Install siege:

	$ brew install siege

- Run following command to do load testing:

    $ siege -c100 -d1 -r100 http://localhost:8080/post

> More information about siege, please refer:<http://www.joedog.org/siege-home/>


## Reference:

- [Vertx.io Eclipse dev environment](http://timtang.me/blog/2013/04/13/vertx-eclipse-dev/)
- [Vertx.io Introduction](http://timtang.me/blog/2013/04/18/vertx-pubsub/)
