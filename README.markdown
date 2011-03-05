Simple RabbitMQ
===============

The goal of this project is to demonstrate a very simple wrapper around the
RabbitMQ API. The wrapper hides away the connection management logic so code
using the wrapper can focus on sending and receiving messages.


Running RabbitMQ
----------------
You can follow the installation instructions at: <http://www.rabbitmq.com/install.html>

On Ubuntu, its as easy as:
    apt-get install rabbitmq-server

One installed, launch a mini-cluster of 2 with the following:
    sudo /etc/init.d/rabbitmq-server stop
	sudo rabbitmq-multi start_all 2

Shut down the cluster with:
    sudo rabbitmq-multi stop_all


Running the Examples
--------------------
Build the project with "ant". All the dependencies are fetched with Ivy. The
basic steps are:

    $ ant
    $ cd target
    $ ./run.sh

The project comes with a number of example classes in src/scala/app. By default,
the run.sh script uses the SingleMessage class. You can specify a different
example as a command line parameter:

    $ ./run.sh FillQueue

Happy experimenting! Contributions are always welcome.

Christoph Neumann
