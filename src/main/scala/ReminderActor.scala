import akka.actor.Cancellable
import akka.util.Timeout
import com.bot4s.telegram.methods.SendMessage
import java.time.{LocalTime, ZoneId}
import java.time.temporal.ChronoUnit
import scala.concurrent.duration._

case class SetReminder(
  chatId: Long,
  text: String,
  hours: Long,
  minutes: Long,
  seconds: Long,
  id: String
)

case class SetTimedReminder(chatId: Long, text: String, time: LocalTime, id: String)

case class SetScheduledReminder(
  chatId: Long,
  text: String,
  initialDelay: LocalTime,
  delay: FiniteDuration,
  id: String
)

case class CancelReminder(id: String)

case class ReminderMessage(chatId: Long, text: String)

class ReminderActor(bot: BasicBot) extends akka.actor.Actor {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val timeout: Timeout = Timeout(5.seconds)
  private val reminders         = Map.empty[String, Cancellable]

  override def receive: Receive = onMessage(reminders)

  override def postStop(): Unit =
    context.system.terminate()

  private def onMessage(reminders: Map[String, Cancellable]): Receive = {
    case ReminderMessage(chatId, text) => bot.request(SendMessage(chatId, "Напоминание: " + text))

    case SetReminder(chatId, text, hours, minutes, seconds, id) =>
      val delay = hours.hours + minutes.minutes + seconds.second
      val cancellable =
        context.system.scheduler.scheduleOnce(delay, self, ReminderMessage(chatId, text))
      context.become(onMessage(reminders + (id -> cancellable)))

    case SetTimedReminder(chatId, text, time, id) =>
      val now   = LocalTime.now(ZoneId.systemDefault())
      val delay = ChronoUnit.SECONDS.between(now, time).seconds
      val cancellable =
        context.system.scheduler.scheduleOnce(delay, self, ReminderMessage(chatId, text))
      context.become(onMessage(reminders + (id -> cancellable)))

    case SetScheduledReminder(chatId, text, initialDelay, delay, id) =>
      val now     = LocalTime.now(ZoneId.systemDefault())
      val initial = ChronoUnit.SECONDS.between(now, initialDelay).seconds
      val cancellable = context.system.scheduler.scheduleWithFixedDelay(
        initial,
        delay,
        self,
        ReminderMessage(chatId, text)
      )
      context.become(onMessage(reminders + (id -> cancellable)))

    case CancelReminder(id) =>
      reminders.get(id).foreach(_.cancel())
      context.become(onMessage(reminders - id))
  }

}
