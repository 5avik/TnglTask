package pro.tngl.tangledtask.domain

import java.time.LocalDate

enum class RepeatRule { NONE, DAILY, WEEKLY, MONTHLY, WEEKDAYS, INTERVAL }

fun nextDate(date: LocalDate, rule: RepeatRule, weekdays: Int = 0, interval: Int = 1, anchor: Int = date.dayOfMonth): LocalDate? =
    when (rule) {
        RepeatRule.NONE -> null
        RepeatRule.DAILY -> date.plusDays(1)
        RepeatRule.WEEKLY -> date.plusWeeks(1)
        RepeatRule.MONTHLY -> date.plusMonths(1).let { it.withDayOfMonth(anchor.coerceIn(1, it.lengthOfMonth())) }
        RepeatRule.WEEKDAYS -> (1L..7L).map(date::plusDays).firstOrNull {
            weekdays and (1 shl (it.dayOfWeek.value - 1)) != 0
        }
        RepeatRule.INTERVAL -> date.plusDays(interval.coerceAtLeast(1).toLong())
    }

data class Streaks(val current: Int, val longest: Int)

fun streaks(dates: Iterable<LocalDate>, today: LocalDate): Streaks {
    val days = dates.toSet().filter { it <= today }.sorted()
    var previous: LocalDate? = null
    var run = 0
    var longest = 0
    for (day in days) {
        run = if (previous?.plusDays(1) == day) run + 1 else 1
        longest = maxOf(longest, run)
        previous = day
    }
    return Streaks(if (previous == today || previous == today.minusDays(1)) run else 0, longest)
}
