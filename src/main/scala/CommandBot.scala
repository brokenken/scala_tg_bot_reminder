import akka.actor.{ActorSystem, Props}
import cats.implicits.toFunctorOps
import com.bot4s.telegram.api.declarative.{Commands, RegexCommands}
import com.bot4s.telegram.future.Polling
import com.bot4s.telegram.methods.SetMyCommands
import com.bot4s.telegram.models.BotCommand

import java.time.LocalTime
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import scala.util.Try

class CommandBot(token: String)
    extends BasicBot(token)
    with Polling
    with Commands[Future]
    with RegexCommands[Future] {

  object Long {
    def unapply(s: String): Option[Long] = Try(s.toLong).toOption
  }

  object String {
    def unapply(s: String): Option[String] = Try(s.toString).toOption
  }

  private def check_time(hh: Long, mm: Long, ss: Long): Boolean =
    if (hh < 0 || hh > 23 || mm < 0 || mm > 59 || ss < 0 || ss > 59)
      false
    else
      true

  implicit val system: ActorSystem = ActorSystem("reminder-system")
  private val reminderActor        = system.actorOf(Props(new ReminderActor(this)), "reminderActor")

  request(
    SetMyCommands(
      List(
        BotCommand(
          "timer_once",
          "Установить таймер 1 раз в формате \"/reminder_once hours minutes seconds text\"" +
            ", где hours minutes second - время до напоминалки, text - текст напоминалки"
        ),
        BotCommand(
          "reminder_once",
          "Установить напоминалку на определённое время (время вводится в формате: hh mm ss)"
        ),
        BotCommand(
          "reminder_schedule",
          "Установить напоминалку по распинсанию. (Формат аргументов: hh_s, mm_s, ss_s," +
            " hh_d, mm_d, ss_d text, hh_s, mm_s, ss_s - время начала напоминания, hh_d mm_d ss_d - время делея," +
            " text - текс напоминалки"
        )
      )
    )
  ).void

  onCommand("/timer_once") { implicit msg =>
    withArgs {
      case Seq(Long(hh), Long(mm), Long(ss), String(text)) =>
        val id = java.util.UUID.randomUUID().toString
        reminderActor ! SetReminder(msg.chat.id, text, hh, mm, ss, id)
        reply("Таймер установлен.")
        reply(s"Для отмены данной напоминалки введите команду:\n/cancel $id").void
      case Seq(Long(mm), Long(ss), String(text)) =>
        val id = java.util.UUID.randomUUID().toString
        reminderActor ! SetReminder(msg.chat.id, text, 0, mm, ss, id)
        reply("Таймер установлен.")
        reply(s"Для отмены данной напоминалки введите команду:\n/cancel $id").void
      case Seq(Long(ss), String(text)) =>
        val id = java.util.UUID.randomUUID().toString
        reminderActor ! SetReminder(msg.chat.id, text, 0, 0, ss, id)
        reply("Таймер установлен.")
        reply(s"Для отмены данной напоминалки введите команду:\n/cancel $id").void
      case _ =>
        reply(
          "Неправильный формат аргументов: \"/timer_once hours minutes seconds text\", " +
            "или \"/timer_once minutes seconds text\", " +
            "или \"/timer_once seconds text\""
        ).void
    }
  }

  onCommand("/reminder_once") { implicit msg =>
    withArgs {
      case Seq(Long(hh), Long(mm), Long(ss), String(text)) =>
        if (!check_time(hh, mm, ss))
          reply("Неправильно указано время").void
        else {
          val time = LocalTime.of(hh.toInt, mm.toInt, ss.toInt)
          val id   = java.util.UUID.randomUUID().toString
          reminderActor ! SetTimedReminder(msg.chat.id, text, time, id)
          reply(s"Напоминалка устанволена на время: $time\n")
          reply(s"Для отмены данной напоминалки введите команду:\n/cancel $id").void
        }
      case _ =>
        reply("Неправильный формат аргументов: \"/reminder_once hours minutes seconds text\"").void
    }
  }

  onCommand("/reminder_schedule") { implicit msg =>
    withArgs {
      case Seq(
            Long(hh_start),
            Long(mm_start),
            Long(ss_start),
            Long(hh_delay),
            Long(mm_delay),
            Long(ss_delay),
            String(text)
          ) =>
        if (check_time(hh_start, mm_start, ss_start)) {
          val initialDelay = LocalTime.of(hh_start.toInt, mm_start.toInt, ss_start.toInt)
          val delay        = hh_delay.hours + mm_delay.minutes + ss_delay.second
          val id           = java.util.UUID.randomUUID().toString
          reminderActor ! SetScheduledReminder(msg.chat.id, text, initialDelay, delay, id)
          reply(
            "Напоминалка по расписанию установлена. \n Для отмены данной напоминалки введите команду:\n"
              + s"/cancel $id"
          ).void
        } else
          reply("Неправильно указано стартовое время").void
      case _ =>
        reply(
          "Неправильный формат аргументов: \"/timer_once hours_start minutes_start seconds_start " +
            "hours_delay minutes_delay seconds_delay text\", " +
            "или \"/timer_once hours minutes hours minutes text\""
        ).void
    }
  }

  onCommand("/cancel") { implicit msg =>
    withArgs { case Seq(String(id)) =>
      reminderActor ! CancelReminder(id)
      reply("Напоминалка отменена: " + id).void
    }
  }

}
