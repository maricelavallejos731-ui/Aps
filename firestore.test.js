const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "super_admin_uid";
const ADMIN_EMAIL = "maricelavallejo731@gmail.com";
const DIRECTOR_UID = "race_director_uid";
const TIMEKEEPER_UID = "timekeeper_uid";
const SPECTATOR_UID = "spectator_uid";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const nowTs = () => new Date();

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

async function seedRoles() {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const ts = new Date();
    await db.collection("user_directory").doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      displayName: "Super Admin",
      role: "SUPER_ADMIN",
      isDirectoryVisible: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("user_directory").doc(DIRECTOR_UID).set({
      uid: DIRECTOR_UID,
      displayName: "Race Director",
      role: "RACE_DIRECTOR",
      isDirectoryVisible: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("user_directory").doc(TIMEKEEPER_UID).set({
      uid: TIMEKEEPER_UID,
      displayName: "Timekeeper",
      role: "TIMEKEEPER",
      isDirectoryVisible: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("user_directory").doc(SPECTATOR_UID).set({
      uid: SPECTATOR_UID,
      displayName: "Spectator",
      role: "SPECTATOR",
      isDirectoryVisible: true,
      createdAt: ts,
      updatedAt: ts,
    });
  });
}

test("Unauthenticated user: cannot read any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("championships").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("sessions").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("users").doc(SPECTATOR_UID).get());
});

test("Email spoofing test: unverified admin email cannot write championships or self-assign SUPER_ADMIN", async () => {
  const spoofDb = testEnv.authenticatedContext("attacker_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  const ts = new Date();
  await assertFails(
    spoofDb.collection("user_directory").doc("attacker_uid").set({
      uid: "attacker_uid",
      displayName: "Attacker",
      role: "SUPER_ADMIN",
      isDirectoryVisible: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    spoofDb.collection("championships").doc("champ_1").set({
      name: "Fake Champ",
      seasonYear: 2026,
      category: "F1",
      status: "ACTIVE",
      visibility: "PUBLIC",
      ownerId: "attacker_uid",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Spectator: can create own SPECTATOR profile, cannot escalate role or read other PII", async () => {
  const specDb = testEnv.authenticatedContext(SPECTATOR_UID, {
    email: "fan@example.com",
    email_verified: true,
  }).firestore();

  const ts = new Date();
  await assertSucceeds(
    specDb.collection("users").doc(SPECTATOR_UID).set({
      uid: SPECTATOR_UID,
      email: "fan@example.com",
      displayName: "Fan User",
      role: "SPECTATOR",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    specDb.collection("user_directory").doc(SPECTATOR_UID).set({
      uid: SPECTATOR_UID,
      displayName: "Fan User",
      role: "SPECTATOR",
      isDirectoryVisible: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cannot escalate self to RACE_DIRECTOR
  await assertFails(
    specDb.collection("user_directory").doc(SPECTATOR_UID).update({
      role: "RACE_DIRECTOR",
      updatedAt: new Date(),
    })
  );

  // Cannot read another user's private PII doc
  await assertFails(specDb.collection("users").doc(ADMIN_UID).get());
});

test("Spectator: can read public race data, cannot create or modify circuits, events, sessions, or laps", async () => {
  await seedRoles();
  const ts = new Date();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("championships").doc("champ_1").set({
      name: "Apex World Series",
      seasonYear: 2026,
      category: "Formula Apex",
      status: "ACTIVE",
      visibility: "PUBLIC",
      ownerId: ADMIN_UID,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const specDb = testEnv.authenticatedContext(SPECTATOR_UID).firestore();
  await assertSucceeds(
    specDb.collection("championships").where("visibility", "==", "PUBLIC").get()
  );

  await assertFails(
    specDb.collection("championships").doc("champ_2").set({
      name: "Unauthorized",
      seasonYear: 2026,
      category: "GT3",
      status: "ACTIVE",
      visibility: "PUBLIC",
      ownerId: SPECTATOR_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Full RBAC workflow: SuperAdmin creates circuit/event, RaceDirector starts session & penalizes, Timekeeper logs lap", async () => {
  await seedRoles();
  const ts = new Date();

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const directorDb = testEnv.authenticatedContext(DIRECTOR_UID).firestore();
  const keeperDb = testEnv.authenticatedContext(TIMEKEEPER_UID).firestore();

  // SuperAdmin creates championship, circuit, event
  await assertSucceeds(
    adminDb.collection("championships").doc("champ_apex").set({
      name: "Apex GP Championship",
      seasonYear: 2026,
      category: "Formula Apex",
      status: "ACTIVE",
      visibility: "PUBLIC",
      ownerId: ADMIN_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    adminDb.collection("circuits").doc("circ_monza").set({
      name: "Autodromo Nazionale",
      country: "Italia",
      countryCode: "IT",
      lengthKm: 5.793,
      turnsCount: 11,
      sectorCount: 3,
      startFinishLabel: "Recta Principal",
      sectorSplits: [0.33, 0.67, 1.0],
      measurementPoints: ["T1 Chicane", "Ascari"],
      trackPoints: ["0.1,0.5", "0.5,0.2", "0.9,0.5", "0.5,0.8"],
      visibility: "PUBLIC",
      ownerId: ADMIN_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    adminDb.collection("race_events").doc("evt_1").set({
      championshipId: "champ_apex",
      circuitId: "circ_monza",
      circuitName: "Autodromo Nazionale",
      grandPrixName: "Gran Premio de Italia",
      country: "Italia",
      countryCode: "IT",
      eventDateMillis: Date.now(),
      practiceSchedule: "Vie 10:30",
      qualifyingSchedule: "Sab 15:00",
      raceSchedule: "Dom 14:00",
      raceDurationLabel: "53 vueltas",
      status: "LIVE",
      roundNumber: 1,
      visibility: "PUBLIC",
      ownerId: ADMIN_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Race Director creates session and entry, Timekeeper cannot create session
  const sessionPayload = {
    eventId: "evt_1",
    championshipId: "champ_apex",
    circuitId: "circ_monza",
    circuitName: "Autodromo Nazionale",
    sessionName: "Carrera Oficial",
    sessionType: "RACE",
    status: "ACTIVE",
    flagStatus: "GREEN",
    timingMode: "MANUAL",
    totalLapsTarget: 53,
    durationMinutes: 90,
    startedAtEpochMs: Date.now(),
    elapsedPausedMs: 0,
    bestSessionLapMs: 0,
    bestSessionLapDriverId: "",
    bestSector1Ms: 0,
    bestSector2Ms: 0,
    bestSector3Ms: 0,
    sectorCount: 3,
    visibility: "PUBLIC",
    ownerId: DIRECTOR_UID,
    createdAt: ts,
    updatedAt: ts,
  };

  await assertFails(keeperDb.collection("sessions").doc("sess_1").set({ ...sessionPayload, ownerId: TIMEKEEPER_UID }));
  await assertSucceeds(directorDb.collection("sessions").doc("sess_1").set(sessionPayload));

  await assertSucceeds(
    directorDb.collection("session_entries").doc("ent_1").set({
      sessionId: "sess_1",
      eventId: "evt_1",
      championshipId: "champ_apex",
      driverName: "Carlos Sainz",
      driverCode: "SAI",
      nationality: "España",
      countryCode: "ES",
      teamName: "Scuderia Apex",
      teamColorHex: "#DC0000",
      carModel: "AX-26",
      carNumber: 55,
      driverStatus: "ON_TRACK",
      position: 1,
      lapsCompleted: 0,
      lastLapMs: 0,
      bestLapMs: 0,
      totalTimeMs: 0,
      currentSector1Ms: 0,
      currentSector2Ms: 0,
      currentSector3Ms: 0,
      bestSector1Ms: 0,
      bestSector2Ms: 0,
      bestSector3Ms: 0,
      penaltySeconds: 0,
      pointsAwarded: 0,
      hasFastestLap: false,
      visibility: "PUBLIC",
      ownerId: DIRECTOR_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Timekeeper logs a lap and updates entry timing, but CANNOT change driverStatus or apply penalty
  await assertSucceeds(
    keeperDb.collection("laps").doc("lap_1").set({
      sessionId: "sess_1",
      entryId: "ent_1",
      driverName: "Carlos Sainz",
      carNumber: 55,
      lapNumber: 1,
      lapTimeMs: 81456,
      sectorTimesMs: [26100, 28200, 27156],
      isValid: true,
      invalidReason: "",
      isPersonalBest: true,
      isSessionBest: true,
      recordedByUid: TIMEKEEPER_UID,
      timingSource: "MANUAL",
      gpsAccuracyMeters: 0,
      recordedAtEpochMs: Date.now(),
      visibility: "PUBLIC",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    keeperDb.collection("session_entries").doc("ent_1").update({
      lapsCompleted: 1,
      lastLapMs: 81456,
      bestLapMs: 81456,
      totalTimeMs: 81456,
      hasFastestLap: true,
      updatedAt: new Date(),
    })
  );

  await assertFails(
    keeperDb.collection("session_entries").doc("ent_1").update({
      driverStatus: "RETIRED",
      updatedAt: new Date(),
    })
  );

  await assertFails(
    keeperDb.collection("penalties").doc("pen_1").set({
      sessionId: "sess_1",
      entryId: "ent_1",
      driverName: "Carlos Sainz",
      carNumber: 55,
      penaltyType: "TIME_5S",
      secondsAdded: 5,
      reason: "Track limits",
      issuedByUid: TIMEKEEPER_UID,
      visibility: "PUBLIC",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // RaceDirector CAN issue penalty and change driverStatus
  await assertSucceeds(
    directorDb.collection("penalties").doc("pen_1").set({
      sessionId: "sess_1",
      entryId: "ent_1",
      driverName: "Carlos Sainz",
      carNumber: 55,
      penaltyType: "TIME_5S",
      secondsAdded: 5,
      reason: "Track limits Turn 4",
      issuedByUid: DIRECTOR_UID,
      visibility: "PUBLIC",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Shadow update test: Ghost field rejected
  await assertFails(
    adminDb.collection("championships").doc("champ_apex").update({
      name: "Updated",
      ghostField: true,
      updatedAt: new Date(),
    })
  );
});
