import bcrypt from 'bcryptjs';
import crypto from 'crypto';
import db, { initDatabase } from '../config/db.js';
import { APP_CONFIG } from '../config/constants.js';

export function runSeed() {
  initDatabase();

  console.log('🌱 Seeding database with realistic production demo dataset...');

  // 1. Clear existing data
  db.exec(`
    DELETE FROM attendance_records;
    DELETE FROM class_sessions;
    DELETE FROM student_enrollments;
    DELETE FROM subjects;
    DELETE FROM teachers;
    DELETE FROM students;
    DELETE FROM academic_calendar;
  `);

  // 2. Create Teachers
  const teacherId1 = 't_rajesh_01';
  const teacherId2 = 't_meenakshi_02';
  const passwordHash = bcrypt.hashSync('password123', 10);

  db.prepare(`
    INSERT INTO teachers (id, name, email, password_hash, department, role)
    VALUES 
      (?, 'Dr. Rajesh Kumar', 'rajesh.kumar@college.edu', ?, 'Computer Science & Engineering', 'TEACHER'),
      (?, 'Prof. Meenakshi Sundaram', 'meenakshi@college.edu', ?, 'Computer Science & Engineering', 'TEACHER')
  `).run(teacherId1, passwordHash, teacherId2, passwordHash);

  // 3. Create Students
  const students = [
    { id: 's_001', name: 'Gulshan', roll: '22CS101', collegeId: 'C2022101', email: 'aarav.sharma@college.edu', phone: '+91 98765 43210' },
    { id: 's_002', name: 'Priya Patel', roll: '22CS102', collegeId: 'C2022102', email: 'priya.patel@college.edu', phone: '+91 98765 43211' },
    { id: 's_003', name: 'Rohan Gupta', roll: '22CS103', collegeId: 'C2022103', email: 'rohan.gupta@college.edu', phone: '+91 98765 43212' },
    { id: 's_004', name: 'Ananya Verma', roll: '22CS104', collegeId: 'C2022104', email: 'ananya.verma@college.edu', phone: '+91 98765 43213' },
    { id: 's_005', name: 'Vikram Malhotra', roll: '22CS105', collegeId: 'C2022105', email: 'vikram.m@college.edu', phone: '+91 98765 43214' }
  ];

  const insertStudent = db.prepare(`
    INSERT INTO students (id, name, roll_number, college_id, email, phone, semester, department)
    VALUES (?, ?, ?, ?, ?, ?, 6, 'Computer Science & Engineering')
  `);

  for (const s of students) {
    insertStudent.run(s.id, s.name, s.roll, s.collegeId, s.email, s.phone);
  }

  // 4. Create Subjects
  const subjects = [
    { id: 'sub_dsa', code: 'CS601', name: 'Data Structures & Algorithms', teacherId: teacherId1, credits: 4 },
    { id: 'sub_dbms', code: 'CS602', name: 'Database Management Systems', teacherId: teacherId1, credits: 4 },
    { id: 'sub_os', code: 'CS603', name: 'Operating Systems', teacherId: teacherId2, credits: 4 },
    { id: 'sub_cn', code: 'CS604', name: 'Computer Networks', teacherId: teacherId2, credits: 4 },
    { id: 'sub_math', code: 'CS605', name: 'Mathematics for Computer Science', teacherId: teacherId1, credits: 3 }
  ];

  const insertSubject = db.prepare(`
    INSERT INTO subjects (id, code, name, teacher_id, semester, credits, department)
    VALUES (?, ?, ?, ?, 6, ?, 'Computer Science & Engineering')
  `);

  for (const sub of subjects) {
    insertSubject.run(sub.id, sub.code, sub.name, sub.teacherId, sub.credits);
  }

  // 5. Enroll Students in Subjects
  const insertEnrollment = db.prepare(`
    INSERT INTO student_enrollments (id, student_id, subject_id, semester)
    VALUES (?, ?, ?, 6)
  `);

  for (const s of students) {
    for (const sub of subjects) {
      insertEnrollment.run(`enr_${s.id}_${sub.id}`, s.id, sub.id);
    }
  }

  // 6. Generate Past 30 Days of Sessions and Attendance Records
  const insertSession = db.prepare(`
    INSERT INTO class_sessions (
      id, subject_id, teacher_id, room_name, date,
      start_time, end_time, latitude, longitude,
      allowed_radius_meters, qr_token, status
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'CLOSED')
  `);

  const insertRecord = db.prepare(`
    INSERT INTO attendance_records (
      id, student_id, session_id, subject_id, status,
      verification_method, location_verified, distance_meters, timestamp
    ) VALUES (?, ?, ?, ?, ?, 'QR_GEOFENCE', 1, ?, ?)
  `);

  let sessionCount = 0;
  // Generate 8 sessions per subject over past 30 days = 40 total classes
  const today = new Date();
  const rooms = ['LH-301 (CSE Wing)', 'LH-302 (CSE Wing)', 'CS Lab 2', 'Smart Hall B'];

  subjects.forEach((sub, subIdx) => {
    for (let dayOffset = 25; dayOffset >= 1; dayOffset -= 3) {
      sessionCount++;
      const sessionDate = new Date(today);
      sessionDate.setDate(today.getDate() - dayOffset);
      const dateStr = sessionDate.toISOString().split('T')[0];
      const startHour = 9 + (subIdx % 4) * 2;
      const startTimeStr = `${startHour < 10 ? '0' + startHour : startHour}:00`;
      const endTimeStr = `${startHour + 1 < 10 ? '0' + (startHour + 1) : startHour + 1}:00`;
      const sessionId = `ses_past_${sub.id}_${dayOffset}`;
      const room = rooms[subIdx % rooms.length];

      insertSession.run(
        sessionId,
        sub.id,
        sub.teacherId,
        room,
        dateStr,
        startTimeStr,
        endTimeStr,
        APP_CONFIG.CAMPUS_DEFAULT_LAT,
        APP_CONFIG.CAMPUS_DEFAULT_LNG,
        20.0,
        'EXPIRED_MOCK_TOKEN'
      );

      // Create attendance records for Aarav Sharma (s_001) targeting ~82.5% overall
      students.forEach((st, stIdx) => {
        let isPresent = true;
        // Deterministic absences for realistic stats
        if (st.id === 's_001') {
          if (sub.id === 'sub_dsa' && (dayOffset === 10 || dayOffset === 22)) isPresent = false;
          if (sub.id === 'sub_dbms' && (dayOffset === 7 || dayOffset === 16 || dayOffset === 25)) isPresent = false;
          if (sub.id === 'sub_os' && dayOffset === 19) isPresent = false;
          if (sub.id === 'sub_math' && dayOffset === 13) isPresent = false;
        } else {
          // Other students
          if ((dayOffset + stIdx) % 5 === 0) isPresent = false;
        }

        if (isPresent) {
          const dist = Math.round((4 + Math.random() * 10) * 10) / 10;
          insertRecord.run(
            `rec_${sessionId}_${st.id}`,
            st.id,
            sessionId,
            sub.id,
            'PRESENT',
            dist,
            `${dateStr}T${startTimeStr}:15.000Z`
          );
        }
      });
    }
  });

  // 7. Create Active Today Session for live testing
  const activeSessionId = 'ses_active_live_dsa';
  const todayStr = today.toISOString().split('T')[0];
  db.prepare(`
    INSERT INTO class_sessions (
      id, subject_id, teacher_id, room_name, date,
      start_time, end_time, latitude, longitude,
      allowed_radius_meters, qr_token, status
    ) VALUES (
      ?, 'sub_dsa', ?, 'LH-301 (CSE Wing)', ?,
      '10:00', '11:00', ?, ?, 20.0, 'LIVE_TOKEN_PLACEHOLDER', 'ACTIVE'
    )
  `).run(
    activeSessionId,
    teacherId1,
    todayStr,
    APP_CONFIG.CAMPUS_DEFAULT_LAT,
    APP_CONFIG.CAMPUS_DEFAULT_LNG
  );

  // 8. Populate Academic Calendar
  const insertCalendar = db.prepare(`
    INSERT OR REPLACE INTO academic_calendar (id, date, is_academic_day, is_holiday, holiday_name, day_of_week)
    VALUES (?, ?, ?, ?, ?, ?)
  `);

  const daysOfWeek = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
  for (let i = -30; i <= 30; i++) {
    const d = new Date(today);
    d.setDate(today.getDate() + i);
    const dStr = d.toISOString().split('T')[0];
    const dayName = daysOfWeek[d.getDay()];
    const isWeekend = d.getDay() === 0 || d.getDay() === 6;
    
    let isHoliday = 0;
    let holidayName = null;
    if (i === 5) { isHoliday = 1; holidayName = 'University Foundation Day'; }
    if (i === 12) { isHoliday = 1; holidayName = 'National Tech Fest'; }
    if (i === 18) { isHoliday = 1; holidayName = 'Autumn Recess'; }

    insertCalendar.run(
      `cal_${dStr}`,
      dStr,
      isWeekend || isHoliday ? 0 : 1,
      isHoliday,
      holidayName,
      dayName
    );
  }

  console.log(`✅ Database successfully seeded with 5 students, 5 subjects, ${sessionCount + 1} sessions, and academic calendar!`);
}

// Execute directly if run via CLI
if (process.argv[1] && process.argv[1].endsWith('seedData.js')) {
  runSeed();
}
