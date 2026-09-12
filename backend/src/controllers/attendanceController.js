import crypto from 'crypto';
import db from '../config/db.js';
import { APP_CONFIG } from '../config/constants.js';
import { verifyGeofence } from '../utils/geofence.js';
import { verifySignedQrToken } from '../utils/tokenUtils.js';

// Global map for Server-Sent Events subscribers (Teacher Dashboards)
export const liveSessionSubscribers = new Map(); // sessionId -> Set of res objects

export function broadcastSessionAttendance(sessionId, eventData) {
  const subscribers = liveSessionSubscribers.get(sessionId);
  if (subscribers && subscribers.size > 0) {
    const payload = `data: ${JSON.stringify(eventData)}\n\n`;
    for (const res of subscribers) {
      try {
        res.write(payload);
      } catch (err) {
        subscribers.delete(res);
      }
    }
  }
}

/**
 * QR Attendance Scanning and Verification Pipeline
 * Validates: Token signature, expiration, session status, student enrollment,
 * mock location integrity, GPS accuracy, Haversine geofence, and duplicate check.
 */
export function scanAttendance(req, res) {
  try {
    console.log("========== ATTENDANCE SCAN ==========");
    console.log("Body:", req.body);

    const studentId = req.user ? req.user.id : req.body.studentId;
    const { qrToken, latitude, longitude, accuracy, isMockLocation } = req.body;

    console.log("Student ID:", studentId);
    console.log("QR Token:", qrToken);
   

    // 1. Validate required fields
    if (!studentId) {
      return res.status(401).json({
        success: false,
        stepFailed: 'STUDENT_AUTH',
        error: 'Student identity verification missing.'
      });
    }

    if (!qrToken) {
      return res.status(400).json({
        success: false,
        stepFailed: 'QR_VALIDATION',
        error: 'QR Token is required.'
      });
    }

  
const tokenResult = verifySignedQrToken(qrToken);
console.log("🔐 QR RESULT:", tokenResult);

    if (!tokenResult.valid) {
      return res.status(400).json({
        success: false,
        stepFailed: 'QR_VALIDATION',
        error: tokenResult.error || 'Invalid Attendance QR',
        expired: tokenResult.expired || false
      });
    }
// console.log("➡️ QR VERIFICATION PASSED");

    const { sessionId } = tokenResult.payload;
    // 5. Verify Class Session
    const session = db.prepare(`
      SELECT cs.*, s.name as subject_name, s.code as subject_code, t.name as teacher_name
      FROM class_sessions cs
      JOIN subjects s ON cs.subject_id = s.id
      JOIN teachers t ON cs.teacher_id = t.id
      WHERE cs.id = ?
    `).get(sessionId);

console.log("📚 SESSION:", session);

    if (!session) {
      return res.status(404).json({
        success: false,
        stepFailed: 'SESSION_VERIFICATION',
        error: 'Class session not found.'
      });
    }

    if (session.status !== 'ACTIVE') {
      return res.status(400).json({
        success: false,
        stepFailed: 'SESSION_VERIFICATION',
        error: `Class session is currently ${session.status.toLowerCase()}. Attendance is not being accepted.`
      });
    }

    // 6. Verify Student Enrollment in the Subject & Device Binding Lock
    const student = db.prepare('SELECT * FROM students WHERE id = ?').get(studentId);
    console.log("👨‍🎓 STUDENT:", student);
    if (!student) {
      return res.status(404).json({
        success: false,
        stepFailed: 'STUDENT_AUTH',
        error: 'Student record not found in system.'
      });
    }

    // --------------------------------------------------
    // Device Binding Security Check (Anti-Proxy)
    // --------------------------------------------------
    const deviceId = req.body.deviceId || req.headers['x-device-id'];
    const deviceModel = req.body.deviceModel || 'Android Device';

    if (deviceId) {
      const cleanDevId = deviceId.trim();

      // Check 1: Is this device already registered to ANOTHER student? (Anti-Proxy Rule)
      const otherStudentWithDevice = db.prepare(`
        SELECT * FROM students 
        WHERE device_id = ? AND id != ?
      `).get(cleanDevId, student.id);

      if (otherStudentWithDevice) {
        console.warn(`🚫 Proxy Attendance Blocked: Device ${cleanDevId} is registered to ${otherStudentWithDevice.name} (${otherStudentWithDevice.roll_number}). Attempted for ${student.name} (${student.roll_number})`);
        return res.status(403).json({
          success: false,
          stepFailed: 'DEVICE_VERIFICATION',
          code: 'DEVICE_ALREADY_IN_USE',
          error: `🚫 Proxy Attendance Blocked: This phone is already registered to ${otherStudentWithDevice.name} (${otherStudentWithDevice.roll_number}). You cannot mark attendance for another student from the same device.`
        });
      }

      // Check 2: Does this device match the student's bound device?
      if (student.device_id && student.device_id !== cleanDevId) {
        console.warn(`🚫 Attendance Device Mismatch for student ${student.roll_number}. Expected: ${student.device_id} (${student.device_model}), Received: ${cleanDevId}`);
        return res.status(403).json({
          success: false,
          stepFailed: 'DEVICE_VERIFICATION',
          code: 'DEVICE_MISMATCH',
          error: `🚫 You can only mark attendance from your registered device (${student.device_model || 'Primary Phone'}). Device mismatch detected.`
        });
      }

      // Check 3: If student had no device bound yet, bind it now!
      if (!student.device_id) {
        db.prepare(`
          UPDATE students 
          SET device_id = ?, device_model = ?, device_bound_at = CURRENT_TIMESTAMP 
          WHERE id = ?
        `).run(cleanDevId, deviceModel, student.id);
        console.log(`🔒 Device automatically locked on attendance for ${student.roll_number}: ${cleanDevId}`);
      }
    } else if (student.device_id) {
      return res.status(403).json({
        success: false,
        stepFailed: 'DEVICE_VERIFICATION',
        code: 'DEVICE_REQUIRED',
        error: `🚫 Device Identification Missing: You must mark attendance from your registered device (${student.device_model || 'Registered Phone'}).`
      });
    }

    const enrollment = db.prepare(`
      SELECT * FROM student_enrollments 
      WHERE student_id = ? AND subject_id = ?
    `).get(studentId, session.subject_id);

    console.log("📖 ENROLLMENT:", enrollment);
    if (!enrollment) {
      return res.status(403).json({
        success: false,
        stepFailed: 'ENROLLMENT_VERIFICATION',
        error: `You are not enrolled in ${session.subject_name} (${session.subject_code}).`
      });
    }

    // 7. Check Duplicate Attendance
    const existingAttendance = db.prepare(`
      SELECT * FROM attendance_records 
      WHERE student_id = ? AND session_id = ?
    `).get(studentId, sessionId);

    console.log("🔁 EXISTING ATTENDANCE:", existingAttendance);
    if (existingAttendance) {
      return res.status(409).json({
        success: false,
        stepFailed: 'DUPLICATE_CHECK',
        error: 'Attendance Already Marked for this session.',
        attendance: {
          id: existingAttendance.id,
          status: existingAttendance.status,
          timestamp: existingAttendance.timestamp,
          distanceMeters: existingAttendance.distance_meters
        }
      });
    }

    // 8. Anti-Spoofing & Geofence Verification
    let locationVerified = false;
    let distanceMeters = null;
    let gpsAccuracy = null;
    let allowedRadius = session.allowed_radius_meters || APP_CONFIG.DEFAULT_GEOFENCE_RADIUS_METERS;

    if (latitude !== undefined && longitude !== undefined) {
      // A. Check mock location
      if (isMockLocation === true) {
        return res.status(400).json({
          success: false,
          stepFailed: 'LOCATION_VERIFICATION',
          error: 'Location verification failed: Suspicious mock / simulated GPS detected. Physical classroom presence is required.'
        });
      }

      // B. Check GPS accuracy
      gpsAccuracy = accuracy !== undefined ? Number(accuracy) : 10;
      if (gpsAccuracy > APP_CONFIG.MAX_ALLOWED_GPS_ACCURACY_METERS) {
        return res.status(400).json({
          success: false,
          stepFailed: 'LOCATION_VERIFICATION',
          error: `Location accuracy is too low (±${Math.round(gpsAccuracy)}m). Please enable High Accuracy GPS.`,
          accuracy: gpsAccuracy,
          maxAllowedAccuracy: APP_CONFIG.MAX_ALLOWED_GPS_ACCURACY_METERS
        });
      }

      // C. Calculate Geodesic Distance
      const geofence = verifyGeofence(
        Number(latitude),
        Number(longitude),
        session.latitude,
        session.longitude,
        allowedRadius
      );
      distanceMeters = geofence.distanceMeters;

      if (!geofence.isWithinGeofence) {
        return res.status(400).json({
          success: false,
          stepFailed: 'GEOFENCE_CHECK',
          error: `❌ Attendance Not Marked: You are approximately ${geofence.distanceMeters} meters away from ${session.room_name}. Required: within ${geofence.allowedRadiusMeters} meters.`,
          distanceMeters: geofence.distanceMeters,
          allowedRadiusMeters: geofence.allowedRadiusMeters,
          classroomName: session.room_name
        });
      }

      locationVerified = true;
    }

    // 9. Record Attendance in Database
    const attendanceId = 'att_' + crypto.randomUUID();
    const nowIso = new Date().toISOString();

    db.prepare(`
      INSERT INTO attendance_records (
        id, student_id, session_id, subject_id, status,
        verification_method, location_verified, distance_meters,
        student_lat, student_lng, gps_accuracy, timestamp
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `).run(
      attendanceId,
      studentId,
      sessionId,
      session.subject_id,
      'PRESENT',
      locationVerified ? 'QR_GEOFENCE' : 'QR',
      locationVerified ? 1 : 0,
      distanceMeters,
      latitude !== undefined ? Number(latitude) : null,
      longitude !== undefined ? Number(longitude) : null,
      gpsAccuracy,
      nowIso
    );

    const recordData = {
      attendanceId,
      studentId: student.id,
      studentName: student.name,
      rollNumber: student.roll_number,
      subjectName: session.subject_name,
      subjectCode: session.subject_code,
      roomName: session.room_name,
      date: session.date,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
      timestamp: nowIso,
      status: 'PRESENT',
      locationVerified,
      distanceMeters,
      allowedRadiusMeters: allowedRadius
    };

    // 10. Broadcast real-time event to Teacher Live Dashboard
    broadcastSessionAttendance(sessionId, {
      type: 'ATTENDANCE_MARKED',
      record: recordData
    });

    console.log("✅ ATTENDANCE SUCCESS"); //
console.log("===================================="); //

    return res.status(200).json({
      success: true,
      message: 'Attendance Marked Successfully',
      attendance: recordData
    });

  } catch (error) {
    console.error('Scan attendance error:', error);
    return res.status(500).json({
      success: false,
      error: 'Failed to process attendance scan. Please try again.'
    });
  }
}
