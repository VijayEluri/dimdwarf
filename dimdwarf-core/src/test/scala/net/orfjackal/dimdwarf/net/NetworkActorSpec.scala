package net.orfjackal.dimdwarf.net

import org.junit.runner.RunWith
import org.hamcrest.Matchers._
import org.hamcrest.MatcherAssert.assertThat
import net.orfjackal.specsy._
import net.orfjackal.dimdwarf.mq.MessageQueue
import net.orfjackal.dimdwarf.util._
import org.apache.mina.core.buffer.IoBuffer
import SimpleSgsProtocolReferenceMessages._
import net.orfjackal.dimdwarf.util.CustomMatchers._
import org.apache.mina.transport.socket.nio.NioSocketConnector
import org.apache.mina.core.service.IoHandlerAdapter
import org.apache.mina.core.session.IoSession
import java.net._

@RunWith(classOf[Specsy])
class NetworkActorSpec extends Spec {
  val TIMEOUT = 100L
  val port = SocketUtil.anyFreePort
  val toHub = new MessageQueue[Any]("toHub")

  val networkActor = new NetworkActor(port, new SimpleSgsProtocolIoHandler(toHub))
  networkActor.start()
  defer {networkActor.stop()}

  val clientReceived = new ByteSink(TIMEOUT)
  val clientSession = connectClient(port, clientReceived)
  defer {clientSession.close(true)}

  "Receives messages from clients and forwards them to the hub" >> {
    clientSends(logoutRequest())

    assertHubReceives(LogoutRequest())
  }

  "Sends messages to clients" >> {
    givenClientHasConnected()

    networkActor.process(LogoutSuccess())

    assertClientReceived(logoutSuccess())
  }


  private def givenClientHasConnected() {
    clientSends(loginRequest("username", "password"))
    assertHubReceives(LoginRequest("username", "password"))
  }

  private def clientSends(message: IoBuffer) {
    clientSession.write(message)
  }

  private def assertClientReceived(expected: IoBuffer) {
    assertEventually(clientReceived, startsWithBytes(expected))
  }

  private def assertHubReceives(expected: Any) {
    assertThat(toHub.poll(TIMEOUT), is(expected))
  }

  private def connectClient(port: Int, receivedMessages: ByteSink): IoSession = {
    val connector = new NioSocketConnector()
    connector.setHandler(new IoHandlerAdapter {
      override def messageReceived(session: IoSession, message: AnyRef) {
        receivedMessages.append(message.asInstanceOf[IoBuffer])
      }
    })
    val connecting = connector.connect(new InetSocketAddress("localhost", port))
    connecting.awaitUninterruptibly(TIMEOUT);
    connecting.getSession
  }
}
