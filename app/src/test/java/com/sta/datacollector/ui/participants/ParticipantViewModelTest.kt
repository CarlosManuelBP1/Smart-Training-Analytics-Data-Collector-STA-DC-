package com.sta.datacollector.ui.participants

import com.sta.datacollector.data.ParticipantRepository
import com.sta.datacollector.data.local.ParticipantDao
import com.sta.datacollector.data.local.ParticipantEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ParticipantViewModelTest {
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var dao: FakeParticipantDao
    private lateinit var viewModel: ParticipantViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dao = FakeParticipantDao()
        viewModel = ParticipantViewModel(
            repository = ParticipantRepository(dao),
            currentTimeMillis = { NOW },
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `empty nickname and invalid numbers show validation errors`() = runTest(testDispatcher) {
        viewModel.startCreating()
        viewModel.updateHeight("ciento setenta")
        viewModel.updateWeight("-5")

        viewModel.saveEditor()

        val editor = viewModel.uiState.value.editor
        assertNotNull(editor)
        assertNotNull(editor!!.nicknameError)
        assertNotNull(editor.heightError)
        assertNotNull(editor.weightError)
        assertTrue(dao.participants.value.isEmpty())
    }

    @Test
    fun `create accepts decimal comma and participant can be selected`() = runTest(testDispatcher) {
        viewModel.startCreating()
        viewModel.updateNickname("  Corredora  ")
        viewModel.updateHeight("171,5")
        viewModel.updateWeight("")
        viewModel.updateExperienceLevel(" Intermedio ")

        viewModel.saveEditor()
        advanceUntilIdle()

        val participant = viewModel.uiState.value.participants.single()
        assertEquals("Corredora", participant.nickname)
        assertEquals(171.5f, participant.heightCm)
        assertNull(participant.weightKg)
        assertEquals("Intermedio", participant.experienceLevel)
        assertEquals(NOW, participant.createdAt)
        assertEquals(NOW, participant.updatedAt)

        viewModel.selectParticipant(participant)
        assertEquals(participant.id, viewModel.uiState.value.selectedParticipantId)
    }

    @Test
    fun `edit preserves creation date and archive clears selection`() = runTest(testDispatcher) {
        val original = ParticipantEntity(
            nickname = "Original",
            createdAt = 100L,
            updatedAt = 100L,
        )
        dao.insert(original)
        advanceUntilIdle()
        viewModel.selectParticipant(original)

        viewModel.startEditing(original)
        viewModel.updateNickname("Actualizado")
        viewModel.saveEditor()
        advanceUntilIdle()

        val updated = viewModel.uiState.value.participants.single()
        assertEquals("Actualizado", updated.nickname)
        assertEquals(100L, updated.createdAt)
        assertEquals(NOW, updated.updatedAt)

        viewModel.requestArchive(updated)
        viewModel.confirmArchive()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.participants.isEmpty())
        assertNull(viewModel.uiState.value.selectedParticipantId)
        assertFalse(dao.getById(updated.id)!!.isActive)
    }

    private companion object {
        const val NOW = 1_800_000_000_000L
    }
}

private class FakeParticipantDao : ParticipantDao {
    val participants = MutableStateFlow<List<ParticipantEntity>>(emptyList())

    override suspend fun insert(participant: ParticipantEntity) {
        check(participants.value.none { it.id == participant.id })
        participants.value = participants.value + participant
    }

    override suspend fun update(participant: ParticipantEntity): Int {
        var updated = false
        participants.value = participants.value.map { existing ->
            if (existing.id == participant.id) {
                updated = true
                participant
            } else {
                existing
            }
        }
        return if (updated) 1 else 0
    }

    override suspend fun getById(id: String): ParticipantEntity? =
        participants.value.firstOrNull { it.id == id }

    override fun observeActiveParticipants(): Flow<List<ParticipantEntity>> =
        participants.map { all -> all.filter(ParticipantEntity::isActive) }

    override suspend fun archive(id: String, updatedAt: Long): Int {
        val participant = getById(id)?.takeIf(ParticipantEntity::isActive) ?: return 0
        return update(participant.copy(isActive = false, updatedAt = updatedAt))
    }
}
