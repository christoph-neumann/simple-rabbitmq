/**
@author Christoph Neumann

An example of a producer sending lots and lots of messages to a queue.
*/

package app

import com.rabbitmq.client.Address
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.QueueingConsumer

import lib.SimpleRabbitMQ


object FillQueue {
	val exchange = "example-exchange"
	val queue = "example"

	def main(args: Array[String]) {
		// Anytime a connection is reestablished, we need to make sure the
		// queue exists.
		val rabbit = new SimpleRabbitMQ({ channel =>
			channel.exchangeDeclare(exchange, "fanout")
			channel.queueDeclare(queue, false, false, false, null)
			channel.queueBind(queue, exchange, "")
		})

		// Open up a connection, create a producer, and send lots of messages. Notice how the
		// producer does not have to worry about trapping connection related errors.
		rabbit.withSession { session =>
			val producer = session.createProducer(exchange, "")
			for ( i <- 1 to 100000 )
			{
				val message = i.toString
				producer.send(message)
//				System.out.println(" [x] Sent '" + message + "'")
				if ( i % 10000 == 0 ) { println(i) }
			}
		}
	}
}
