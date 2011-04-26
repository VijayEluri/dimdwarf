package net.orfjackal.dimdwarf.net

import net.orfjackal.dimdwarf.mq.MessageSender
import net.orfjackal.dimdwarf.controller._
import net.orfjackal.dimdwarf.auth._
import net.orfjackal.dimdwarf.net.sgs._
import javax.inject.Inject

@ControllerScoped
class NetworkController @Inject()(toNetwork: MessageSender[NetworkMessage], authenticator: Authenticator) extends Controller {
  def process(message: Any) {
    message match {
      case ReceivedFromClient(message, session) =>
        processClientMessage(message, session)
      case _ =>
    }
  }

  private def processClientMessage(message: ClientMessage, session: SessionHandle) {
    message match {
      case LoginRequest(username, password) =>
        authenticator.isUserAuthenticated(new PasswordCredentials(username, password),
          onYes = {toNetwork.send(SendToClient(LoginSuccess(), session))},
          onNo = {toNetwork.send(SendToClient(LoginFailure(), session))})

      case SessionMessage(message) =>
      // TODO: handle the message

      case LogoutRequest() =>
        toNetwork.send(SendToClient(LogoutSuccess(), session))

      case _ =>
        // TODO: do something smart, maybe disconnect the client if it sends a not allowed message
        assert(false, "Unsupported message: " + message)
    }
  }
}
