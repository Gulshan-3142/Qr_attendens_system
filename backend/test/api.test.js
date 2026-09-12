import assert from 'assert';
import http from 'http';
import { runSeed } from '../src/seed/seedData.js';
import { calculateHaversineDistance, verifyGeofence } from '../src/utils/geofence.js';
import { generateSignedQrToken, verifySignedQrToken } from '../src/utils/tokenUtils.js';

console.log('🧪 Starting SmartQR Attendance Backend Test Suite...');

// 1. Test Geofence Distance Calculation (Haversine)
console.log('\n[1/7] Testing Haversine Geofence Calculation...');
const campusLat = 28.613900;
const campusLng = 77.209000;

// ~8.4m away
const closeLat = 28.613960;
const closeLng = 77.209040;
const distClose = calculateHaversineDistance(campusLat, campusLng, closeLat, closeLng);
console.log(`  📍 Student Close Distance: ${distClose}m`);
assert(distClose <= 20, 'Close student should be within 20m geofence');

// ~100m away
const farLat = 28.614800;
const farLng = 77.209000;
const distFar = calculateHaversineDistance(campusLat, campusLng, farLat, farLng);
console.log(`  📍 Student Far Distance: ${distFar}m`);
assert(distFar > 20, 'Far student should be outside 20m geofence');

const geofencePass = verifyGeofence(closeLat, closeLng, campusLat, campusLng, 20);
assert.strictEqual(geofencePass.isWithinGeofence, true);

const geofenceFail = verifyGeofence(farLat, farLng, campusLat, campusLng, 20);
assert.strictEqual(geofenceFail.isWithinGeofence, false);
console.log('  ✅ Geofence tests passed successfully.');

// 2. Test Dynamic Signed QR Tokens
console.log('\n[2/7] Testing Cryptographic QR Token Signatures & Expiry...');
const tokenObj = generateSignedQrToken('test_session_123', 60);
console.log('  🔑 Generated Signed Token:', tokenObj.token.slice(0, 30) + '...');

const verifiedValid = verifySignedQrToken(tokenObj.token);
assert.strictEqual(verifiedValid.valid, true);
assert.strictEqual(verifiedValid.payload.sessionId, 'test_session_123');

// Test tampered token
const tamperedToken = tokenObj.token.replace(/a/g, 'b');
const verifiedTampered = verifySignedQrToken(tamperedToken);
assert.strictEqual(verifiedTampered.valid, false);

// Test expired token
const expiredTokenObj = generateSignedQrToken('test_session_123', -5); // already expired 5s ago
const verifiedExpired = verifySignedQrToken(expiredTokenObj.token);
assert.strictEqual(verifiedExpired.valid, false);
assert.strictEqual(verifiedExpired.expired, true);
console.log('  ✅ Token security & anti-tamper verification passed.');

// 3. Test Database Seeding & Integration
console.log('\n[3/7] Initializing Database & Seed Data...');
runSeed();
console.log('  ✅ Database initialized and pre-seeded.');

// 4. Test Calculator Math Logic
console.log('\n[4/7] Testing Attendance Calculators Math...');

// A. Leave Calculator: 33 present, 40 total, target 75%
// L <= (33 - 0.75 * 40) / 0.75 = (33 - 30) / 0.75 = 3 / 0.75 = 4
const P = 33, T = 40, k = 0.75;
const maxLeave = Math.floor((P - k * T) / k);
console.log(`  🧮 Max Leave for 33/40 at 75%: ${maxLeave} classes`);
assert.strictEqual(maxLeave, 4, 'Should be able to miss exactly 4 classes');

// B. Target Calculator: 28 present, 40 total (70%), target 75%
// C >= (0.75 * 40 - 28) / (1 - 0.75) = (30 - 28) / 0.25 = 2 / 0.25 = 8
const P2 = 28, T2 = 40, k2 = 0.75;
const requiredConsecutive = Math.ceil((k2 * T2 - P2) / (1 - k2));
console.log(`  🧮 Required Consecutive for 28/40 to reach 75%: ${requiredConsecutive} classes`);
assert.strictEqual(requiredConsecutive, 8, 'Should require 8 consecutive classes');

// C. Exact Day Planner: 8 classes required, 5 classes/day => ceil(8 / 5) = 2 days
const daysReq = Math.ceil(requiredConsecutive / 5.0);
console.log(`  📅 Academic Days Required: ${daysReq} days`);
assert.strictEqual(daysReq, 2, 'Should require 2 academic days');

console.log('  ✅ Calculator mathematical formulas verified.');

// 5. Test Live API Endpoints via HTTP
console.log('\n[5/7] Testing API Endpoints...');

process.env.NODE_ENV = 'test';

async function runApiTests() {
  const { default: app } = await import('../src/server.js');
  
  const testPort = 3099;
  const testServer = await new Promise((resolve) => {
    const s = app.listen(testPort, '127.0.0.1', () => resolve(s));
  });

  const baseUrl = `http://127.0.0.1:${testPort}`;

  const postJson = (url, body, token) => new Promise((resolve, reject) => {
    const data = JSON.stringify(body);
    const u = new URL(url);
    const req = http.request({
      hostname: u.hostname,
      port: u.port,
      path: u.pathname + u.search,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(data),
        ...(token && { 'Authorization': `Bearer ${token}` })
      }
    }, res => {
      let raw = '';
      res.on('data', chunk => raw += chunk);
      res.on('end', () => {
        try { resolve({ status: res.statusCode, data: JSON.parse(raw) }); }
        catch (e) { resolve({ status: res.statusCode, raw }); }
      });
    });
    req.on('error', reject);
    req.write(data);
    req.end();
  });

  const getJson = (url, token) => new Promise((resolve, reject) => {
    const u = new URL(url);
    const req = http.request({
      hostname: u.hostname,
      port: u.port,
      path: u.pathname + u.search,
      method: 'GET',
      headers: {
        ...(token && { 'Authorization': `Bearer ${token}` })
      }
    }, res => {
      let raw = '';
      res.on('data', chunk => raw += chunk);
      res.on('end', () => {
        try { resolve({ status: res.statusCode, data: JSON.parse(raw) }); }
        catch (e) { resolve({ status: res.statusCode, raw }); }
      });
    });
    req.on('error', reject);
    req.end();
  });

  // Test 1: Student Verification
  console.log('  🔍 Testing Student Verification (/api/auth/student/verify)...');
  const verifyRes = await postJson(`${baseUrl}/api/auth/student/verify`, {
    rollNumber: '22CS101',
    collegeId: 'C2022101'
  });
  assert.strictEqual(verifyRes.status, 200);
  assert.strictEqual(verifyRes.data.student.name, 'Aarav Sharma');
  const studentToken = verifyRes.data.token;
  const studentId = verifyRes.data.student.id;
  console.log('  ✅ Student verified. Token acquired.');

  // Test 2: Student Summary & Subject-wise attendance
  console.log('  📊 Testing Student Attendance Summary & Subjects...');
  const summaryRes = await getJson(`${baseUrl}/api/student/attendance/summary?studentId=${studentId}`, studentToken);
  assert.strictEqual(summaryRes.status, 200);
  console.log(`  📈 Current Overall Attendance: ${summaryRes.data.summary.attendancePercentage}% (Present: ${summaryRes.data.summary.presentClasses}, Total: ${summaryRes.data.summary.totalClasses})`);

  const subjectsRes = await getJson(`${baseUrl}/api/student/attendance/subjects?studentId=${studentId}`, studentToken);
  assert.strictEqual(subjectsRes.status, 200);
  console.log(`  📚 Subjects fetched: ${subjectsRes.data.subjects.length} subjects found.`);

  // Test 3: Teacher Login and Create Session
  console.log('  👨‍🏫 Testing Teacher Login & Session Creation...');
  const teacherLoginRes = await postJson(`${baseUrl}/api/auth/teacher/login`, {
    email: 'rajesh.kumar@college.edu',
    password: 'password123'
  });
  assert.strictEqual(teacherLoginRes.status, 200);
  const teacherToken = teacherLoginRes.data.token;

  const sessionRes = await postJson(`${baseUrl}/api/teacher/sessions/create`, {
    subjectId: 'sub_dsa',
    roomName: 'LH-301 (CSE Wing)',
    latitude: 28.613900,
    longitude: 77.209000,
    allowedRadiusMeters: 20,
    qrValiditySeconds: 60
  }, teacherToken);
  assert.strictEqual(sessionRes.status, 201);
  const liveSession = sessionRes.data.session;
  console.log(`  🎯 Created Live Session: ${liveSession.id}, QR Token: ${liveSession.qrToken.slice(0, 20)}...`);

  // Test 4: Geofence Scan - In Zone (Distance ~8.4m <= 20m)
  console.log('  📍 Testing QR Scan WITHIN Geofence (Distance ~8.4m)...');
  const scanPassRes = await postJson(`${baseUrl}/api/attendance/scan`, {
    studentId: studentId,
    qrToken: liveSession.qrToken,
    latitude: 28.613960,
    longitude: 77.209040,
    accuracy: 8,
    isMockLocation: false
  }, studentToken);
  assert.strictEqual(scanPassRes.status, 200);
  assert.strictEqual(scanPassRes.data.attendance.status, 'PRESENT');
  console.log(`  ✅ Attendance successfully marked! Distance: ${scanPassRes.data.attendance.distanceMeters}m`);

  // Test 5: Duplicate Scan Prevention
  console.log('  🔒 Testing Duplicate Scan Prevention...');
  const scanDupRes = await postJson(`${baseUrl}/api/attendance/scan`, {
    studentId: studentId,
    qrToken: liveSession.qrToken,
    latitude: 28.613960,
    longitude: 77.209040,
    accuracy: 8,
    isMockLocation: false
  }, studentToken);
  assert.strictEqual(scanDupRes.status, 409);
  console.log('  ✅ Duplicate scan correctly rejected (409 Conflict).');

  // Test 6: Geofence Scan - Out of Zone (Distance ~100m > 20m) for Student 2
  console.log('  🚫 Testing QR Scan OUTSIDE Geofence (Distance ~100m)...');
  const scanFailRes = await postJson(`${baseUrl}/api/attendance/scan`, {
    studentId: 's_002',
    qrToken: liveSession.qrToken,
    latitude: 28.614800,
    longitude: 77.209000,
    accuracy: 6,
    isMockLocation: false
  });
  assert.strictEqual(scanFailRes.status, 400);
  assert.strictEqual(scanFailRes.data.stepFailed, 'GEOFENCE_CHECK');
  console.log(`  ✅ Out of zone scan rejected as expected: ${scanFailRes.data.error}`);

  // Test 7: Anti-Spoofing Mock Location Check
  console.log('  🛡️ Testing Anti-Spoofing (Mock Location = true)...');
  const mockScanRes = await postJson(`${baseUrl}/api/attendance/scan`, {
    studentId: 's_003',
    qrToken: liveSession.qrToken,
    latitude: 28.613960,
    longitude: 77.209040,
    accuracy: 5,
    isMockLocation: true
  });
  assert.strictEqual(mockScanRes.status, 400);
  console.log('  ✅ Spoofed / Mock location successfully rejected.');

  // Test 8: Device Binding & Security Lock Tests
  console.log('\n  🔒 [NEW] Testing Single Device Binding & Hardware Lock...');
  
  // A. Register/Verify Student with Device 1
  const devStudentRes = await postJson(`${baseUrl}/api/auth/student/verify`, {
    rollNumber: '22CS104',
    collegeId: 'C2022104',
    deviceId: 'samsung_s22_hardware_id_99',
    deviceModel: 'Samsung Galaxy S22'
  });
  assert.strictEqual(devStudentRes.status, 200);
  assert.strictEqual(devStudentRes.data.student.deviceId, 'samsung_s22_hardware_id_99');
  assert.strictEqual(devStudentRes.data.student.isDeviceBound, true);
  console.log('  ✅ Initial Device Lock bound to Samsung Galaxy S22.');

  // B. Attempt Login from a DIFFERENT device (Device 2) -> Should be REJECTED (403)
  const rogueLoginRes = await postJson(`${baseUrl}/api/auth/student/verify`, {
    rollNumber: '22CS104',
    collegeId: 'C2022104',
    deviceId: 'different_phone_id_888',
    deviceModel: 'OnePlus 11'
  });
  assert.strictEqual(rogueLoginRes.status, 403);
  assert.strictEqual(rogueLoginRes.data.code, 'DEVICE_LOCKED');
  console.log('  ✅ Rogue login from different device blocked with 403 Forbidden.');

  // C. Attendance scan from MISMATCHED device -> Should be REJECTED (403)
  const rogueScanRes = await postJson(`${baseUrl}/api/attendance/scan`, {
    studentId: 's_004',
    qrToken: liveSession.qrToken,
    latitude: 28.613960,
    longitude: 77.209040,
    accuracy: 8,
    isMockLocation: false,
    deviceId: 'different_phone_id_888'
  });
  assert.strictEqual(rogueScanRes.status, 403);
  assert.strictEqual(rogueScanRes.data.stepFailed, 'DEVICE_VERIFICATION');
  console.log('  ✅ Attendance from mismatched device rejected with 403 Forbidden.');

  // D. Admin Reset Device Binding (POST /api/teacher/students/:id/reset-device)
  console.log('  👨‍🏫 Testing Faculty Reset Device Binding (/api/teacher/students/:id/reset-device)...');
  const resetDevRes = await postJson(`${baseUrl}/api/teacher/students/s_004/reset-device`, {}, teacherToken);
  assert.strictEqual(resetDevRes.status, 200);
  assert.strictEqual(resetDevRes.data.success, true);
  console.log('  ✅ Admin successfully cleared hardware lock for student.');

  // E. Student can now bind their new phone (OnePlus 11)
  const newPhoneLoginRes = await postJson(`${baseUrl}/api/auth/student/verify`, {
    rollNumber: '22CS104',
    collegeId: 'C2022104',
    deviceId: 'different_phone_id_888',
    deviceModel: 'OnePlus 11'
  });
  assert.strictEqual(newPhoneLoginRes.status, 200);
  assert.strictEqual(newPhoneLoginRes.data.student.deviceId, 'different_phone_id_888');
  assert.strictEqual(newPhoneLoginRes.data.student.deviceModel, 'OnePlus 11');
  console.log('  ✅ New phone successfully bound after admin reset.');

  // F. [ANTI-PROXY CRITICAL] Student 2 (Vikram - 22CS105) tries to log in on the SAME Phone (different_phone_id_888)
  console.log('  🛡️ Testing Anti-Proxy: Another student attempting login on already registered phone...');
  const multiStudentProxyLoginRes = await postJson(`${baseUrl}/api/auth/student/verify`, {
    rollNumber: '22CS105',
    collegeId: 'C2022105',
    deviceId: 'different_phone_id_888',
    deviceModel: 'OnePlus 11'
  });
  assert.strictEqual(multiStudentProxyLoginRes.status, 403);
  assert.strictEqual(multiStudentProxyLoginRes.data.code, 'DEVICE_ALREADY_IN_USE');
  console.log(`  ✅ Anti-Proxy successfully blocked multi-student login on same device: ${multiStudentProxyLoginRes.data.error}`);

  // G. [ANTI-PROXY CRITICAL] Student 2 tries to mark attendance using Student 1's phone
  console.log('  🛡️ Testing Anti-Proxy: Another student attempting attendance scan from already registered phone...');
  const multiStudentProxyScanRes = await postJson(`${baseUrl}/api/attendance/scan`, {
    studentId: 's_005',
    qrToken: liveSession.qrToken,
    latitude: 28.613960,
    longitude: 77.209040,
    accuracy: 8,
    isMockLocation: false,
    deviceId: 'different_phone_id_888'
  });
  assert.strictEqual(multiStudentProxyScanRes.status, 403);
  assert.strictEqual(multiStudentProxyScanRes.data.code, 'DEVICE_ALREADY_IN_USE');
  console.log(`  ✅ Anti-Proxy successfully blocked attendance scan from already bound device.`);

  console.log('\n=======================================================');
  console.log('🎉 ALL BACKEND & ANTI-PROXY DEVICE LOCK TESTS PASSED WITH 100% SUCCESS!');
  console.log('=======================================================\n');
  process.exit(0);
}

runApiTests().catch(err => {
  console.error('❌ Test suite error:', err);
  process.exit(1);
});
