import db from '../config/db.js';

/**
 * Get Student Profile Information
 */
export function getStudentProfile(req, res) {
  try {
    const studentId = req.user ? req.user.id : req.query.studentId;
    if (!studentId) {
      return res.status(400).json({ success: false, error: 'Student ID required.' });
    }

    const student = db.prepare('SELECT * FROM students WHERE id = ?').get(studentId);
    if (!student) {
      return res.status(404).json({ success: false, error: 'Student not found.' });
    }

    const enrolledSubjects = db.prepare(`
      SELECT s.*, t.name as teacher_name
      FROM student_enrollments se
      JOIN subjects s ON se.subject_id = s.id
      LEFT JOIN teachers t ON s.teacher_id = t.id
      WHERE se.student_id = ?
    `).all(studentId);

    return res.status(200).json({
      success: true,
      student: {
        id: student.id,
        name: student.name,
        rollNumber: student.roll_number,
        collegeId: student.college_id,
        email: student.email,
        phone: student.phone,
        department: student.department,
        semester: student.semester,
        avatarUrl: student.avatar_url,
        verified: true,
        enrolledSubjects
      }
    });
  } catch (error) {
    console.error('Get student profile error:', error);
    return res.status(500).json({ success: false, error: 'Failed to retrieve profile.' });
  }
}

/**
 * Calculate Authoritative Overall Attendance Summary
 */
export function getAttendanceSummary(req, res) {
  try {
    const studentId = req.user ? req.user.id : req.query.studentId;
    if (!studentId) {
      return res.status(400).json({ success: false, error: 'Student ID required.' });
    }

    // 1. All sessions held for enrolled subjects of this student
    const totalSessions = db.prepare(`
      SELECT cs.id, cs.subject_id, cs.date
      FROM class_sessions cs
      JOIN student_enrollments se ON cs.subject_id = se.subject_id
      WHERE se.student_id = ?
    `).all(studentId);

    const totalClasses = totalSessions.length;

    // 2. Marked present / late records
    const attendanceRecords = db.prepare(`
      SELECT ar.*
      FROM attendance_records ar
      WHERE ar.student_id = ?
    `).all(studentId);

    const presentClasses = attendanceRecords.filter(r => r.status === 'PRESENT' || r.status === 'LATE').length;
    const absentClasses = Math.max(0, totalClasses - presentClasses);

    const attendancePercentage = totalClasses > 0 
      ? Math.round((presentClasses / totalClasses) * 1000) / 10 
      : 100.0;

    // 3. Academic Days Calculation
    const academicDaysRecord = db.prepare(`
      SELECT COUNT(DISTINCT date) as count 
      FROM academic_calendar 
      WHERE is_academic_day = 1 AND date <= CURRENT_DATE
    `).get();

    const distinctSessionDays = db.prepare(`
      SELECT COUNT(DISTINCT date) as count
      FROM class_sessions cs
      JOIN student_enrollments se ON cs.subject_id = se.subject_id
      WHERE se.student_id = ?
    `).get(studentId).count;

    const academicDays = Math.max(1, academicDaysRecord.count > 0 ? academicDaysRecord.count : distinctSessionDays);
    const averageClassesPerDay = Math.round((totalClasses / academicDays) * 10) / 10;

    return res.status(200).json({
      success: true,
      summary: {
        attendancePercentage,
        presentClasses,
        absentClasses,
        totalClasses,
        academicDays,
        averageClassesPerDay: averageClassesPerDay || 5.0,
        statusLabel: attendancePercentage >= 75 ? 'Safe / Eligible' : 'Low Attendance Alert'
      }
    });
  } catch (error) {
    console.error('Get attendance summary error:', error);
    return res.status(500).json({ success: false, error: 'Failed to compute attendance summary.' });
  }
}

/**
 * Subject-Wise Attendance Breakdown
 */
export function getSubjectWiseAttendance(req, res) {
  try {
    const studentId = req.user ? req.user.id : req.query.studentId;
    if (!studentId) {
      return res.status(400).json({ success: false, error: 'Student ID required.' });
    }

    const enrolledSubjects = db.prepare(`
      SELECT s.id, s.code, s.name, s.credits, s.semester, t.name as teacher_name
      FROM student_enrollments se
      JOIN subjects s ON se.subject_id = s.id
      LEFT JOIN teachers t ON s.teacher_id = t.id
      WHERE se.student_id = ?
    `).all(studentId);

    const result = enrolledSubjects.map(sub => {
      const totalHeld = db.prepare(`
        SELECT COUNT(*) as count FROM class_sessions WHERE subject_id = ?
      `).get(sub.id).count;

      const presentCount = db.prepare(`
        SELECT COUNT(*) as count FROM attendance_records 
        WHERE student_id = ? AND subject_id = ? AND status IN ('PRESENT', 'LATE')
      `).get(studentId, sub.id).count;

      const absentCount = Math.max(0, totalHeld - presentCount);
      const percentage = totalHeld > 0 ? Math.round((presentCount / totalHeld) * 1000) / 10 : 100.0;

      let trend = 'STABLE';
      if (percentage >= 80) trend = 'UP';
      else if (percentage < 75) trend = 'DOWN';

      return {
        subjectId: sub.id,
        subjectCode: sub.code,
        subjectName: sub.name,
        teacherName: sub.teacher_name || 'Department Faculty',
        credits: sub.credits,
        semester: sub.semester,
        presentClasses: presentCount,
        absentClasses: absentCount,
        totalClasses: totalHeld,
        attendancePercentage: percentage,
        trend,
        isLowAttendance: percentage < 75
      };
    });

    return res.status(200).json({
      success: true,
      subjects: result
    });
  } catch (error) {
    console.error('Subject-wise error:', error);
    return res.status(500).json({ success: false, error: 'Failed to retrieve subject-wise attendance.' });
  }
}

/**
 * Attendance History with Filtering and Pagination
 */
export function getAttendanceHistory(req, res) {
  try {
    const studentId = req.user ? req.user.id : req.query.studentId;
    const { subjectId, status, startDate, endDate, page = 1, limit = 20 } = req.query;

    if (!studentId) {
      return res.status(400).json({ success: false, error: 'Student ID required.' });
    }

    let query = `
      SELECT 
        ar.id as attendance_id,
        ar.status,
        ar.timestamp,
        ar.verification_method,
        ar.distance_meters,
        cs.date,
        cs.start_time,
        cs.end_time,
        cs.room_name,
        s.id as subject_id,
        s.code as subject_code,
        s.name as subject_name,
        t.name as teacher_name
      FROM attendance_records ar
      JOIN class_sessions cs ON ar.session_id = cs.id
      JOIN subjects s ON ar.subject_id = s.id
      LEFT JOIN teachers t ON cs.teacher_id = t.id
      WHERE ar.student_id = ?
    `;

    const params = [studentId];

    if (subjectId) {
      query += ` AND ar.subject_id = ?`;
      params.push(subjectId);
    }

    if (status) {
      query += ` AND ar.status = ?`;
      params.push(status.toUpperCase());
    }

    if (startDate) {
      query += ` AND cs.date >= ?`;
      params.push(startDate);
    }

    if (endDate) {
      query += ` AND cs.date <= ?`;
      params.push(endDate);
    }

    query += ` ORDER BY cs.date DESC, cs.start_time DESC LIMIT ? OFFSET ?`;
    const offset = (Number(page) - 1) * Number(limit);
    params.push(Number(limit), offset);

    const records = db.prepare(query).all(...params);

    const totalCount = db.prepare(`
      SELECT COUNT(*) as count FROM attendance_records WHERE student_id = ?
    `).get(studentId).count;

    return res.status(200).json({
      success: true,
      page: Number(page),
      limit: Number(limit),
      totalRecords: totalCount,
      records: records.map(r => ({
        attendanceId: r.attendance_id,
        date: r.date,
        time: r.start_time,
        subjectId: r.subject_id,
        subjectCode: r.subject_code,
        subjectName: r.subject_name,
        teacherName: r.teacher_name,
        roomName: r.room_name,
        status: r.status,
        distanceMeters: r.distance_meters,
        verificationMethod: r.verification_method,
        timestamp: r.timestamp
      }))
    });
  } catch (error) {
    console.error('Get history error:', error);
    return res.status(500).json({ success: false, error: 'Failed to retrieve attendance history.' });
  }
}

/**
 * Attendance Analytics Trends (7 Days, 30 Days, Month, Semester)
 */
export function getAttendanceTrends(req, res) {
  try {
    const studentId = req.user ? req.user.id : req.query.studentId;
    const { period = '7days' } = req.query; // '7days', '30days', 'month', 'semester'

    if (!studentId) {
      return res.status(400).json({ success: false, error: 'Student ID required.' });
    }

    let daysBack = 7;
    if (period === '30days') daysBack = 30;
    else if (period === 'month') daysBack = 31;
    else if (period === 'semester') daysBack = 120;

    const query = `
      SELECT 
        cs.date,
        COUNT(cs.id) as total_held,
        SUM(CASE WHEN ar.status IN ('PRESENT', 'LATE') THEN 1 ELSE 0 END) as present_count
      FROM class_sessions cs
      JOIN student_enrollments se ON cs.subject_id = se.subject_id
      LEFT JOIN attendance_records ar ON cs.id = ar.session_id AND ar.student_id = ?
      WHERE se.student_id = ? AND cs.date >= date('now', '-${daysBack} days')
      GROUP BY cs.date
      ORDER BY cs.date ASC
    `;

    const rawStats = db.prepare(query).all(studentId, studentId);

    const trendData = rawStats.map(row => ({
      date: row.date,
      totalClasses: row.total_held,
      present: row.present_count || 0,
      absent: Math.max(0, row.total_held - (row.present_count || 0)),
      percentage: row.total_held > 0 ? Math.round(((row.present_count || 0) / row.total_held) * 100) : 100
    }));

    return res.status(200).json({
      success: true,
      period,
      trends: trendData
    });
  } catch (error) {
    console.error('Trends error:', error);
    return res.status(500).json({ success: false, error: 'Failed to retrieve attendance trends.' });
  }
}
