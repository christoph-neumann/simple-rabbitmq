/**
@author Christoph Neumann

An example of a single message being sent from a producer to a consumer.
*/

package app

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.QueueingConsumer

import lib.SimpleRabbitMQ


object SingleMessage {
	val queue = "example"

	def main(args: Array[String]) {
		// Anytime a connection is reestablished, we need to make sure the
		// queue exists.
		val rabbit = new SimpleRabbitMQ({ channel =>
			channel.queueDeclare(queue, false, false, false, null)
		})

		// The "session" is a way of hiding all the connection (and reconnection) details. We use it
		// to create a safe Producer and a Consumer.
		rabbit.withSession { session =>
			val producer = session.createProducer("", queue)
			val consumer = session.createConsumer(queue, true)
			val message = "Take me to your leader!"

			println("Sending: " + message)
			producer.send(message)

			val got = consumer.receive()
			println("Received: "+ got)
		}
	}
}
