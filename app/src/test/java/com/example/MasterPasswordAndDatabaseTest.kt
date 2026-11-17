package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ApexLocalDatabase
import com.example.data.local.LocalPreferencesDataStore
import com.example.data.local.MasterPasswordHasher
import com.example.data.model.TimingMode
import com.example.data.repository.ApexTimingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MasterPasswordAndDatabaseTest {

    private lateinit var context: Context
    private lateinit var db: ApexLocalDatabase
    private lateinit var repository: ApexTimingRepository
    private lateinit var prefs: LocalPreferencesDataStore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, ApexLocalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ApexTimingRepository(db.timingDao(), db.pendingLapDao())
        prefs = LocalPreferencesDataStore(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun masterPasswordHasher_verifiesInitialPasswordAndCustomSalts() {
        assertTrue(
            MasterPasswordHasher.verify(
                input = "APEX731M",
                salt = MasterPasswordHasher.INITIAL_SALT,
                expectedHash = MasterPasswordHasher.INITIAL_HASH
            )
        )
        assertFalse(
            MasterPasswordHasher.verify(
                input = "wrong_password",
                salt = MasterPasswordHasher.INITIAL_SALT,
                expectedHash = MasterPasswordHasher.INITIAL_HASH
            )
        )
        assertFalse(
            MasterPasswordHasher.verify(
                input = "",
                salt = MasterPasswordHasher.INITIAL_SALT,
                expectedHash = MasterPasswordHasher.INITIAL_HASH
            )
        )

        val salt = MasterPasswordHasher.generateSalt()
        val customHash = MasterPasswordHasher.hashPassword(salt, "MiClaveSecreta2026")
        assertTrue(MasterPasswordHasher.verify("MiClaveSecreta2026", salt, customHash))
        assertFalse(MasterPasswordHasher.verify("APEX731M", salt, customHash))
    }

    @Test
    fun localPreferencesDataStore_validatesAndUpdatesMasterPassword() = runBlocking {
        assertTrue(prefs.verifyMasterPassword("APEX731M"))
        assertFalse(prefs.verifyMasterPassword("1234"))
        assertFalse(prefs.hasCustomMasterPassword.first())

        val tooShort = prefs.updateMasterPassword("APEX731M", "123")
        assertTrue(tooShort.isFailure)

        val wrongCurrent = prefs.updateMasterPassword("INCORRECT", "NuevaClave99")
        assertTrue(wrongCurrent.isFailure)

        val updated = prefs.updateMasterPassword("APEX731M", "NuevaClave99")
        assertTrue(updated.isSuccess)
        assertTrue(prefs.hasCustomMasterPassword.first())
        assertTrue(prefs.verifyMasterPassword("NuevaClave99"))
        assertFalse(prefs.verifyMasterPassword("APEX731M"))
    }

    @Test
    fun localRepository_seedsStructureAndRecordsRealLapWithoutSimulatedTimes() = runBlocking {
        repository.ensureInitialStructureIfEmpty()

        val champs = repository.observeChampionships().first()
        val circuits = repository.observeCircuits().first()
        val sessions = repository.observeSessions().first()
        assertEquals(1, champs.size)
        assertEquals(1, circuits.size)
        assertEquals(1, sessions.size)

        val session = sessions.first()
        val entriesBefore = repository.observeAllSessionEntries().first()
        assertTrue(entriesBefore.isEmpty())

        val addResult = repository.addSessionEntry(
            session = session,
            driverName = "C. Sainz",
            driverCode = "SAI",
            nationality = "España",
            countryCode = "ES",
            teamName = "Williams Racing",
            teamColorHex = "#00A0DE",
            carModel = "FW47",
            carNumber = 55L,
            initialPosition = 1L
        )
        assertTrue(addResult.isSuccess)

        val entries = repository.observeAllSessionEntries().first()
        assertEquals(1, entries.size)
        val entry = entries.first()

        val lapResult = repository.recordLap(
            session = session,
            entry = entry,
            allSessionEntries = entries,
            lapTimeMs = 78_450L,
            sectorTimesMs = listOf(24_100L, 28_200L, 26_150L),
            timingSource = TimingMode.MANUAL.name,
            gpsAccuracyMeters = 0.0
        )
        assertTrue(lapResult.isSuccess)

        val updatedEntry = repository.observeAllSessionEntries().first().first()
        assertEquals(1L, updatedEntry.lapsCompleted)
        assertEquals(78_450L, updatedEntry.bestLapMs)
        assertEquals(24_100L, updatedEntry.bestSector1Ms)
        assertEquals(28_200L, updatedEntry.bestSector2Ms)
        assertEquals(26_150L, updatedEntry.bestSector3Ms)
        assertTrue(updatedEntry.hasFastestLap)
    }
}
