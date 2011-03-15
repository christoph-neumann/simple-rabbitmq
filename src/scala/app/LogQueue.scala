/**
@author Christoph Neumann

A logger that will receive every message on the queue. It will not be part of the round robin.
*/

package app

import lib.SimpleRabbitMQ


object LogQueue {
	val exchange = "example-exchange"
	var my_queue = "LogQueue"

	def main(args: Array[String]) {
		var verbose: Boolean = args.contains("-v")

		// Anytime a connection is reestablished, we need to make sure the queue exists.
		val rabbit = new SimpleRabbitMQ({ channel =>
			channel.exchangeDeclare(exchange, "fanout")
			val result = channel.queueDeclare(my_queue, false, true, true, null)   // a private queue
			channel.queueBind(my_queue, exchange, "")
		})

		// Open up a connection, create a consumer, and wait for messages.  Because this is bound to
		// its own private queue, it will get its own copy of each message that is sent to the
		// exchange.
		rabbit.withSession { session =>
			val consumer = session.createConsumer(my_queue, true)
			while (true) {
				val message = consumer.receive()
				if ( verbose ) {
					System.out.println("Received: '" + message + "'")
				} else {
					var i = message.toInt
					if ( i % 10000 == 0 ) { println(i) }
				}
			}
		}
	}
}
