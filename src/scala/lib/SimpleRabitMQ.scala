/**
@author Christoph Neumann

Class to simplify the usage of RabbitMQ with connection failover.
*/

package lib

import com.rabbitmq.client.Address
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.QueueingConsumer


class SimpleRabbitMQ( onConnect: Channel => Unit = _=>() ) {
	val factory: ConnectionFactory = new ConnectionFactory()

	// Hardcode the configuration to a cluster of two brokers on localhost.
	val addresses = Array(
		new Address("localhost", 5672),
		new Address("localhost", 5673)
	)

	// Since these have to be recreated whenever the connection drops, we have methods for
	// initializing these.
	var connection: Connection = null
	var channel: Channel = null

	// A safe producer that will silently handle failover.
	private class Producer(exchange: String, route: String) {
		def send(message: String) {
			withSafeConnection { channel =>
				channel.basicPublish(exchange, route, null, message.getBytes())
			}
		}
	}

	// A safe consumer that will silently handle failover.
	private class Consumer(queue: String, autoAck: Boolean) {
		var consumer: QueueingConsumer = null
		initConsumer()

		// Wait for a message. We assume it's a text message.
		def receive(): String = {
			var message: QueueingConsumer.Delivery = null

			// Dequeue a message. This will automatically retry if any exceptions are encountered in
			// the process.
			withSafeConnection({ channel =>
				message = consumer.nextDelivery()
			}, { channel =>
				// Since the consumer maintains a reference to the channel, we have to make a new
				// one after a reconnect.
				initConsumer()
			})

			return new String(message.getBody())
		}

		private def initConsumer() {
			consumer = new QueueingConsumer(channel)
			channel.basicConsume(queue, autoAck, consumer)
		}
	}

	// The "Session" is our own abstraction to create safe Producers and Consumers that don't have
	// to be worried about the details of connection handling.
	private class Session {
		def createProducer(exchange: String, route: String): Producer = {
			return new Producer(exchange, route)
		}

		def createConsumer(queue: String, autoAck: Boolean): Consumer = {
			return new Consumer(queue, autoAck)
		}
	}

	// This is the key method for the class. It will setup the connection and provide the closure
	// with a "Session" it can use to create safe Consumers and Producers. Whenever the closure is
	// finished, this will shutdown the connection.
	def withSession(closure: (Session => Unit)) {
		connect()
		try {
			closure(new Session())
		} finally {
			disconnect()
		}
	}

	// This will execute the given closure and handle any connection or broker related issues that
	// occur. Since the closure may be executed more than once, it should be idempotent. Also, since
	// a connection may be transparently reestablished, the onConnect closure is needed to do any
	// first time initialization.
	private def withSafeConnection(closure: (Channel => Unit), onConnect: (Channel => Unit) = _ => ()) {
		var success = false
		while ( ! success ) {
			try {
				closure(channel)
				success = true
			} catch {
				// Should be one of:
				//   com.rabbitmq.client.AlreadyClosedException
				//   com.rabbitmq.client.ShutdownSignalException
				//   java.net.SocketException
				// but we catch them all...muhahaha!
				case e: Exception => {
					disconnect()
					connect()
					onConnect(channel)
				}
			}
		}
	}

	// Try to connect to the message brokers and create a channel. This can throw an exception if it
	// can't connect.
	private def connect() {
		while ( connection == null ) {
			try {
				print("Attempting to connect...")
				connection = factory.newConnection(addresses)
				println("connected!")
				channel = connection.createChannel()

				// Perform any one-time setup
				onConnect(channel)
			} catch {
				case e: java.net.ConnectException => {
					disconnect() // in case we get a connection, but not a channel
					println("")
					println("Unable to connect. Waiting...")
					Thread.sleep(1000)
				}
			}
		}
	}

	// Try to close everything down. It's possible that the channel and connection are already
	// closed, so these may throw exceptions complaining about it.
	private def disconnect() {
		if ( channel != null ) {
			try { channel.close() } catch { case _ => () }
			channel = null
		}
		if ( connection != null ) {
			try { connection.close() } catch { case _ => () }
			connection = null
		}
	}
}
