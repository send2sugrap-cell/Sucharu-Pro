package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource
import com.sucharu.sucharupro.data.model.task.TaskStatus
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskEndToEndTest {

    private lateinit var notifRepo: NotificationRepositoryImpl
    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        notifRepo = NotificationRepositoryImpl(FakeNotificationDataSource())
        repository = TaskRepositoryImpl(FakeTaskDataSource(), notificationRepository = notifRepo)
    }

    @Test
    fun `test complete end to end staff workflow lifecycle scenario`() = runBlocking {
        // 1. ADMIN creates task
        val createRes = repository.createTask(
            projectId = "PRJ-01",
            title = "E2E Printing Proof Verification",
            description = "Verify color calibration on Heidelberg Offset Press",
            actorUserId = "USR-ADMIN",
            callerRole = UserRole.ADMIN
        )
        assertTrue(createRes.isSuccess)
        val taskId = createRes.getOrNull()!!.taskId

        // 2. MANAGER assigns task to STAFF
        val assignRes = repository.assignTask("PRJ-01", taskId, "USR-STAFF", "Press operator assigned", "USR-MGR", UserRole.MANAGER)
        assertTrue(assignRes.isSuccess)

        // 3. STAFF receives notification
        val notifs = notifRepo.getUserNotifications("PRJ-01", "USR-STAFF", actorId = "USR-ADMIN", callerRole = UserRole.ADMIN).getOrNull()!!
        assertTrue(notifs.isNotEmpty())

        // 4. STAFF acknowledges task
        val ackRes = repository.acknowledgeTask("PRJ-01", taskId, "USR-STAFF", UserRole.STAFF)
        assertTrue(ackRes.isSuccess)

        // 5. STAFF starts task
        val startRes = repository.startTask("PRJ-01", taskId, "USR-STAFF", UserRole.STAFF)
        assertTrue(startRes.isSuccess)

        // 6. STAFF updates progress
        val progRes = repository.updateProgress("PRJ-01", taskId, 50, "Completed initial strip test", "USR-STAFF", UserRole.STAFF)
        assertTrue(progRes.isSuccess)

        // 7. STAFF adds comment & mentions teammate
        val commRes = repository.addComment("PRJ-01", taskId, "Ink density looks high. Checking with QC.", "USR-STAFF", UserRole.STAFF)
        assertTrue(commRes.isSuccess)

        val mentionRes = repository.addMention("PRJ-01", taskId, "USR-QC1", commRes.getOrNull()!!.commentId, "USR-STAFF", UserRole.STAFF)
        assertTrue(mentionRes.isSuccess)

        // 8. Task becomes BLOCKED due to ink issue
        val blockRes = repository.blockTask("PRJ-01", taskId, "Ink viscosity imbalance", "USR-STAFF", UserRole.STAFF)
        assertTrue(blockRes.isSuccess)
        assertEquals(TaskStatus.BLOCKED, blockRes.getOrNull()!!.status)

        // 9. MANAGER unblocks
        val unblockRes = repository.unblockTask("PRJ-01", taskId, "Ink adjusted with solvent", "USR-MGR", UserRole.MANAGER)
        assertTrue(unblockRes.isSuccess)

        // 10. STAFF resumes and completes task
        val compRes = repository.completeTask("PRJ-01", taskId, "Job press run finished", "USR-STAFF", UserRole.STAFF)
        assertTrue(compRes.isSuccess)
        assertEquals(100, compRes.getOrNull()!!.progressPercentage)

        // 11. MANAGER verifies task
        val verifyRes = repository.verifyTask("PRJ-01", taskId, "Press sheets verified ok", "USR-MGR", UserRole.MANAGER)
        assertTrue(verifyRes.isSuccess)

        // 12. MANAGER closes task
        val closeRes = repository.closeTask("PRJ-01", taskId, "Archived", "USR-MGR", UserRole.MANAGER)
        assertTrue(closeRes.isSuccess)
        assertEquals(TaskStatus.CLOSED, closeRes.getOrNull()!!.status)

        // 13. Audit timeline verification
        val auditHistory = repository.getActivityHistory("PRJ-01", taskId, "USR-ADMIN", UserRole.ADMIN).getOrNull()!!
        assertTrue(auditHistory.size >= 10)
    }
}
