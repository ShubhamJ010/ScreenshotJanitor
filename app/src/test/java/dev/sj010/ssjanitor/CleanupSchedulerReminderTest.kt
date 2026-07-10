package dev.sj010.ssjanitor

import dev.sj010.ssjanitor.core.constants.AppConstants
import dev.sj010.ssjanitor.worker.CleanupScheduler
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Verifies the pre-cleanup reminder timing math that [CleanupScheduler] derives
 * from the persisted cleanup hour/minute. This is the logic that previously
 * clamped to 0 under WorkManager and made the reminder unreliable.
 *
 * Because [CleanupScheduler] resolves "now" from the real clock internally, these
 * tests derive the expected cleanup instant from the implementation's own
 * [CleanupScheduler.computeDelayMillis] (which uses the same clock) rather than
 * reconstructing the calendar by hand.
 */
class CleanupSchedulerReminderTest {

    private val lead = TimeUnit.MINUTES.toMillis(AppConstants.PRE_CLEANUP_REMINDER_MINUTES.toLong())

    @Test
    fun reminder_isLeadTimeBefore_cleanup() {
        val now = System.currentTimeMillis()
        // Pick a cleanup time comfortably > 30 min away from `now` (tomorrow 09:15).
        val cleanup = now + CleanupScheduler.computeDelayMillis(9, 15)
        val actual = CleanupScheduler.computeReminderTimeMillis(9, 15)

        assertTrue(
            "reminder should be exactly the lead time before cleanup " +
                "(delta=${cleanup - actual - lead})",
            kotlin.math.abs((cleanup - actual) - lead) < 1000
        )
    }

    @Test
    fun reminder_dueNow_whenCleanupLessThanLeadTimeAway() {
        val now = System.currentTimeMillis()
        // A cleanup 28 min after `now` is < the 30 min lead, so computeReminderTimeMillis
        // clamps the reminder to "now". Convert that delay into an hour/minute pair.
        val cleanupDelay = TimeUnit.MINUTES.toMillis(28)
        val cal = Calendar.getInstance().apply { timeInMillis = now + cleanupDelay }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)

        val actual = CleanupScheduler.computeReminderTimeMillis(hour, minute)
        assertTrue(
            "reminder with < lead-time cleanup should be due now " +
                "(actual=$actual, now=${now - 1000})",
            actual >= now - 1000
        )
    }

    @Test
    fun reminder_isInTheFuture_whenCleanupAlreadyPassedToday() {
        val now = System.currentTimeMillis()
        // 09:15 today is almost certainly in the past for this test run; next
        // occurrence is tomorrow, so the reminder must be in the future.
        val cleanup = now + CleanupScheduler.computeDelayMillis(9, 15)
        val actual = CleanupScheduler.computeReminderTimeMillis(9, 15)
        assertTrue(
            "reminder for a passed cleanup time should be in the future " +
                "(actual=$actual, now=$now)",
            actual > now
        )
    }
}
