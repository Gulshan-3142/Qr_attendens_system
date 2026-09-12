import QRCode from 'qrcode';
import crypto from 'crypto';
import db from '../config/db.js';
import { APP_CONFIG } from '../config/constants.js';
import { generateSignedQrToken } from '../utils/tokenUtils.js';
import { liveSessionSubscribers } from './attendanceController.js';

/**
 * Create a new Class Session with GPS Coordinates & Geofence Radius
 */
export async function createSession(req, res) {
  try {
    const teacherId = req.user ? req.user.id : (req.body.teacherId || 't_rajesh_01');
    const {
      subjectId,
      roomName,
      latitude,
      longitude,
      allowedRadiusMeters = APP_CONFIG.DEFAULT_GEOFENCE_RADIUS_METERS,
      qrValiditySeconds = APP_CONFIG.DEFAULT_QR_VALIDITY_SECONDS,
      date = new Date().toISOString().split('T')[0],
      startTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false }),
      endTime = '23:59'
    } = req.body;

    if (!subjectId || !roomName || latitude === undefined || longitude === undefined) {
      return res.status(400).json({
        success: false,
        error: 'subjectId, roomName, latitude, and longitude are required to create an attendance session.'
      });
    }

    const sessionId = 'ses_' + crypto.randomUUID();
    const tokenData = generateSignedQrToken(sessionId, Number(qrValiditySeconds));

    // Generate QR Code Data URL image
    const qrDataUrl = await QRCode.toDataURL(tokenData.token, {
      errorCorrectionLevel: 'H',
      margin: 2,
      width: 400,
      color: {
        dark: '#0f172a',
        light: '#ffffff'
      }
    });

    db.prepare(`
      INSERT INTO class_sessions (
        id, subject_id, teacher_id, room_name, date,
        start_time, end_time, latitude, longitude,
        allowed_radius_meters, qr_token, qr_expires_at, status
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `).run(
      sessionId,
      subjectId,
      teacherId,
      roomName.trim(),
      date,
      startTime,
      endTime,
      Number(latitude),
      Number(longitude),
      Number(allowedRadiusMeters),
      tokenData.token,
      tokenData.expiresAt,
      'ACTIVE'
    );

    const created = db.prepare(`
      SELECT cs.*, s.name as subject_name, s.code as subject_code
      FROM class_sessions cs
      JOIN subjects s ON cs.subject_id = s.id
      WHERE cs.id = ?
    `).get(sessionId);

    return res.status(201).json({
      success: true,
      message: 'Attendance Session created successfully.',
      session: {
        ...created,
        qrToken: tokenData.token,
        qrExpiresAt: tokenData.expiresAt,
        qrValiditySeconds: Number(qrValiditySeconds),
        qrDataUrl
      }
    });
  } catch (error) {
    console.error('Create session error:', error);
    return res.status(500).json({
      success: false,
      error: 'Failed to create class session.'
    });
  }
}

/**
 * Generate/Refresh Dynamic QR Token for an Active Session
 */
export async function refreshSessionQr(req, res) {
  try {
    const { id } = req.params;
    const { validitySeconds = APP_CONFIG.DEFAULT_QR_VALIDITY_SECONDS } = req.body;

    const session = db.prepare('SELECT * FROM class_sessions WHERE id = ?').get(id);
    if (!session) {
      return res.status(404).json({ success: false, error: 'Session not found.' });
    }

    if (session.status !== 'ACTIVE') {
      return res.status(400).json({ success: false, error: 'Cannot refresh QR for an inactive session.' });
    }

    const tokenData = generateSignedQrToken(id, Number(validitySeconds));
    const qrDataUrl = await QRCode.toDataURL(tokenData.token, {
      errorCorrectionLevel: 'H',
      margin: 2,
      width: 400,
      color: {
        dark: '#0f172a',
        light: '#ffffff'
      }
    });

    db.prepare(`
      UPDATE class_sessions 
      SET qr_token = ?, qr_expires_at = ?
      WHERE id = ?
    `).run(tokenData.token, tokenData.expiresAt, id);

    return res.status(200).json({
      success: true,
      sessionId: id,
      qrToken: tokenData.token,
      qrExpiresAt: tokenData.expiresAt,
      qrValiditySeconds: Number(validitySeconds),
      qrDataUrl
    });
  } catch (error) {
    console.error('Refresh QR error:', error);
    return res.status(500).json({ success: false, error: 'Failed to refresh QR token.' });
  }
}

/**
 * Get Real-Time Live Attendance for a Session
 */
export function getLiveSessionAttendance(req, res) {
  try {
    const { id } = req.params;

    const session = db.prepare(`
      SELECT cs.*, s.name as subject_name, s.code as subject_code, t.name as teacher_name
      FROM class_sessions cs
      JOIN subjects s ON cs.subject_id = s.id
      JOIN teachers t ON cs.teacher_id = t.id
      WHERE cs.id = ?
    `).get(id);

    if (!session) {
      return res.status(404).json({ success: false, error: 'Session not found.' });
    }

    // Get all enrolled students for this subject
    const enrolledStudents = db.prepare(`
      SELECT s.id, s.name, s.roll_number, s.college_id, s.avatar_url, s.device_id, s.device_model, s.device_bound_at
      FROM students s
      JOIN student_enrollments se ON s.id = se.student_id
      WHERE se.subject_id = ?
      ORDER BY s.roll_number ASC
    `).all(session.subject_id);

    // Get marked attendance records for this session
    const markedRecords = db.prepare(`
      SELECT ar.*, s.name as student_name, s.roll_number, s.college_id
      FROM attendance_records ar
      JOIN students s ON ar.student_id = s.id
      WHERE ar.session_id = ?
      ORDER BY ar.timestamp DESC
    `).all(id);

    const markedMap = new Map(markedRecords.map(r => [r.student_id, r]));

    const studentsWithStatus = enrolledStudents.map(st => {
      const record = markedMap.get(st.id);
      return {
        studentId: st.id,
        name: st.name,
        rollNumber: st.roll_number,
        collegeId: st.college_id,
        avatarUrl: st.avatar_url,
        deviceId: st.device_id,
        deviceModel: st.device_model,
        deviceBoundAt: st.device_bound_at,
        isDeviceBound: !!st.device_id,
        status: record ? record.status : 'ABSENT',
        distanceMeters: record ? record.distance_meters : null,
        verificationMethod: record ? record.verification_method : null,
        markedAt: record ? record.timestamp : null
      };
    });

    const totalStudents = enrolledStudents.length;
    const presentCount = markedRecords.filter(r => r.status === 'PRESENT' || r.status === 'LATE').length;
    const absentCount = totalStudents - presentCount;
    const attendancePercentage = totalStudents > 0 ? Math.round((presentCount / totalStudents) * 1000) / 10 : 0;

    return res.status(200).json({
      success: true,
      session,
      metrics: {
        totalStudents,
        presentCount,
        absentCount,
        attendancePercentage
      },
      recentCheckins: markedRecords.slice(0, 10),
      students: studentsWithStatus
    });
  } catch (error) {
    console.error('Get live attendance error:', error);
    return res.status(500).json({ success: false, error: 'Failed to retrieve live attendance.' });
  }
}

/**
 * Close/End an Attendance Session
 */
export function closeSession(req, res) {
  try {
    const { id } = req.params;
    db.prepare(`UPDATE class_sessions SET status = 'CLOSED' WHERE id = ?`).run(id);

    return res.status(200).json({
      success: true,
      message: 'Session closed successfully.'
    });
  } catch (error) {
    console.error('Close session error:', error);
    return res.status(500).json({ success: false, error: 'Failed to close session.' });
  }
}

/**
 * Manual Attendance Toggle by Teacher
 */
export function toggleManualAttendance(req, res) {
  try {
    const { id: sessionId } = req.params;
    const { studentId, status = 'PRESENT' } = req.body;

    const session = db.prepare('SELECT * FROM class_sessions WHERE id = ?').get(sessionId);
    if (!session) {
      return res.status(404).json({ success: false, error: 'Session not found.' });
    }

    const student = db.prepare('SELECT * FROM students WHERE id = ?').get(studentId);
    if (!student) {
      return res.status(404).json({ success: false, error: 'Student not found.' });
    }

    const existing = db.prepare(`
      SELECT * FROM attendance_records WHERE student_id = ? AND session_id = ?
    `).get(studentId, sessionId);

    if (existing) {
      if (status === 'ABSENT') {
        db.prepare('DELETE FROM attendance_records WHERE id = ?').run(existing.id);
      } else {
        db.prepare(`
          UPDATE attendance_records 
          SET status = ?, verification_method = 'MANUAL_TEACHER'
          WHERE id = ?
        `).run(status, existing.id);
      }
    } else if (status !== 'ABSENT') {
      const attendanceId = 'att_' + crypto.randomUUID();
      db.prepare(`
        INSERT INTO attendance_records (
          id, student_id, session_id, subject_id, status,
          verification_method, location_verified, distance_meters, timestamp
        ) VALUES (?, ?, ?, ?, ?, 'MANUAL_TEACHER', 1, 0, CURRENT_TIMESTAMP)
      `).run(attendanceId, studentId, sessionId, session.subject_id, status);
    }

    return res.status(200).json({
      success: true,
      message: `Student attendance updated to ${status}.`
    });
  } catch (error) {
    console.error('Manual attendance error:', error);
    return res.status(500).json({ success: false, error: 'Failed to update manual attendance.' });
  }
}

/**
 * Server-Sent Events (SSE) Stream for Live Teacher Dashboard
 */
export function sessionSseStream(req, res) {
  const { id } = req.params;

  res.writeHead(200, {
    'Content-Type': 'text/event-stream',
    'Cache-Control': 'no-cache',
    'Connection': 'keep-alive'
  });

  res.write(`data: ${JSON.stringify({ type: 'CONNECTED', sessionId: id })}\n\n`);

  if (!liveSessionSubscribers.has(id)) {
    liveSessionSubscribers.set(id, new Set());
  }
  liveSessionSubscribers.get(id).add(res);

  req.on('close', () => {
    const subs = liveSessionSubscribers.get(id);
    if (subs) {
      subs.delete(res);
      if (subs.size === 0) liveSessionSubscribers.delete(id);
    }
  });
}
