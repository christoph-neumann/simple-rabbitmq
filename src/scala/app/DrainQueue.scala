/**
@author Christoph Neumann

An example of a consumer draining all the messages from a queue. 
*/

package app

import com.rabbitmq.client.Address
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.QueueingConsumer

import lib.SimpleRabbitMQ


object DrainQueue {
	val exchange = "example-exchange"
	val queue = "example"

	def main(args: Array[String]) {
		var verbose: Boolean = args.contains("-v")

		// Anytime a connection is reestablished, we need to make sure the
		// queue exists.
		val rabbit = new SimpleRabbitMQ({ channel =>
			channel.exchangeDeclare(exchange, "fanout")
			channel.queueDeclare(queue, false, false, false, null)
			channel.queueBind(queue, exchange, "")
		})

		// Open up a connection, create a consumer, and wait for messages. Notice how the consumer
		// does not have to worry about trapping connection related errors.
		rabbit.withSession { session =>
			val consumer = session.createConsumer(queue, true)
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
