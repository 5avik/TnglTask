package pro.tngl.tangledtask

import org.junit.Assert.*
import org.junit.Test
import pro.tngl.tangledtask.domain.*
import java.time.LocalDate

class PlanningTest {
    @Test fun monthlyRepeatKeepsOriginalDayAfterFebruary() {
        val january = LocalDate.of(2025, 1, 31)
        val february = nextDate(january, RepeatRule.MONTHLY, anchor = 31)!!
        assertEquals(LocalDate.of(2025, 2, 28), february)
        assertEquals(LocalDate.of(2025, 3, 31), nextDate(february, RepeatRule.MONTHLY, anchor = 31))
    }

    @Test fun selectedWeekdaysWrapAndIntervalsUseCalendarDays() {
        val friday = LocalDate.of(2025, 1, 3)
        assertEquals(friday.plusDays(3), nextDate(friday, RepeatRule.WEEKDAYS, weekdays = 1))
        assertEquals(friday.plusWeeks(1), nextDate(friday, RepeatRule.WEEKLY))
        assertEquals(friday.plusDays(14), nextDate(friday, RepeatRule.INTERVAL, interval = 14))
        assertNull(nextDate(friday, RepeatRule.WEEKDAYS))
        assertNull(nextDate(friday, RepeatRule.NONE))
    }

    @Test fun streakUsesDistinctDaysAndIgnoresFuture() {
        val today = LocalDate.of(2025, 1, 10)
        val days = listOf(9L, 8, 7, 2, 1, 1, -1).map(today::minusDays)
        assertEquals(Streaks(2, 3), streaks(days, today))
        assertEquals(Streaks(0, 3), streaks(days, today.plusDays(3)))
        assertEquals(Streaks(0, 0), streaks(emptyList(), today))
    }
}
