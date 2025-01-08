import com.bot4s.telegram.api.RequestHandler
import com.bot4s.telegram.clients.FutureSttpClient
import com.bot4s.telegram.future.TelegramBot
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend

import scala.concurrent.Future

abstract class BasicBot(val token: String) extends TelegramBot {

  implicit val backend: SttpBackend[Future, Any] = AsyncHttpClientFutureBackend()
  override val client: RequestHandler[Future]    = new FutureSttpClient(token)

}
