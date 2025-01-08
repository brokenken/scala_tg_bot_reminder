import pureconfig._
import pureconfig.generic.auto._

case class TelegramConfig(botToken: String)

object TelegramBot extends App {

  private val config = ConfigSource.default.loadOrThrow[TelegramConfig]
  private val token  = config.botToken
  private val bot    = new CommandBot(token)
  bot.run()

}
