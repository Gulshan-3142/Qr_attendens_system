import jwt from 'jsonwebtoken';
import bcrypt from 'bcryptjs';
import crypto from 'crypto';
import db from '../config/db.js';
import { APP_CONFIG } from '../config/constants.js';

/**
 * Student Onboarding & Verification
 *
 * First time:
 *   Student details -> Create student in database -> JWT
 *
 * Next time:
 *   Roll Number + College ID -> Find existing student -> JWT
 */
export function verifyStudent(req, res) {
  try {
    const { name, rollNumber, collegeId, email, phone, deviceId, deviceModel } = req.body;

    // Roll number and College ID are required
    if (!rollNumber || !collegeId) {
      return res.status(400).json({
        success: false,
        error: 'University Roll Number and College ID are mandatory.'
      });
    }

    const cleanRoll = rollNumber.trim().toUpperCase();
    const cleanCollegeId = collegeId.trim().toUpperCase();

    const cleanName = name?.trim().toLowerCase().replace(/^./, char => char.toUpperCase()) || null;
    const cleanEmail = email?.trim().toLowerCase() || null;
    const cleanPhone = phone?.trim() || null;
    const cleanDeviceId = deviceId?.trim() || null;
    const cleanDeviceModel = deviceModel?.trim() || 'Android Device';

    // --------------------------------------------------
    // 1. Anti-Proxy Global Device Check (1 Phone = 1 Student Only)
    // --------------------------------------------------
    if (cleanDeviceId) {
      const otherStudentWithDevice = db.prepare(`
        SELECT * FROM students 
        WHERE device_id = ? 
        AND UPPER(roll_number) != ?
      `).get(cleanDeviceId, cleanRoll);

      if (otherStudentWithDevice) {
        console.warn(`🚫 Anti-Proxy Violation: Device ${cleanDeviceId} is already bound to ${otherStudentWithDevice.name} (${otherStudentWithDevice.roll_number}). Attempted by ${cleanRoll}`);
        return res.status(403).json({
          success: false,
          error: `🚫 Anti-Proxy Security Lock: This mobile device is already bound to another student (${otherStudentWithDevice.name} - ${otherStudentWithDevice.roll_number}). A single phone cannot be used to log in or mark attendance for multiple students.`,
          code: 'DEVICE_ALREADY_IN_USE',
          registeredToStudent: otherStudentWithDevice.name,
          registeredToRoll: otherStudentWithDevice.roll_number
        });
      }
    }

    // --------------------------------------------------
    // 2. Check whether student already exists
    // --------------------------------------------------
    let student = db.prepare(`
      SELECT * FROM students
      WHERE UPPER(roll_number) = ?
      AND UPPER(college_id) = ?
    `).get(cleanRoll, cleanCollegeId);

    // --------------------------------------------------
    // 3. First-time student -> create new record with Device Binding
    // --------------------------------------------------
    if (!student) {

      if (!cleanName) {
        return res.status(400).json({
          success: false,
          error: 'Name is required for first-time student registration.'
        });
      }

      const studentId = 's_' + crypto.randomUUID();

      try {
        db.prepare(`
          INSERT INTO students (
            id,
            name,
            roll_number,
            college_id,
            email,
            phone,
            department,
            semester,
            device_id,
            device_model,
            device_bound_at
          )
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ${cleanDeviceId ? 'CURRENT_TIMESTAMP' : 'NULL'})
        `).run(
          studentId,
          cleanName,
          cleanRoll,
          cleanCollegeId,
          cleanEmail,
          cleanPhone,
          'Computer Science & Engineering',
          6,
          cleanDeviceId,
          cleanDeviceModel
        );

        console.log(
          `✅ New student registered with Device Binding: ${cleanName} (${cleanRoll}) -> Device: ${cleanDeviceId || 'None'}`
        );

        student = db.prepare(`
          SELECT * FROM students
          WHERE id = ?
        `).get(studentId);

      } catch (insertError) {

        // Handle UNIQUE constraint race/duplicate
        if (insertError.code === 'SQLITE_CONSTRAINT_UNIQUE') {
          student = db.prepare(`
            SELECT * FROM students
            WHERE UPPER(roll_number) = ?
            OR UPPER(college_id) = ?
          `).get(cleanRoll, cleanCollegeId);

          if (!student) {
            throw insertError;
          }
        } else {
          throw insertError;
        }
      }

    } else {

      // ------------------------------------------------
      // 4. Existing student -> Check Device Lock
      // ------------------------------------------------
      if (student.device_id && cleanDeviceId && student.device_id !== cleanDeviceId) {
        console.warn(`⚠️ Device Lock Violation for ${student.roll_number}: Bound to ${student.device_id} (${student.device_model}), Attempted from ${cleanDeviceId} (${cleanDeviceModel})`);
        return res.status(403).json({
          success: false,
          error: `🚫 Device Lock Violation: This account is already locked to your registered device (${student.device_model || 'Primary Phone'}). You cannot log in from a different device. Please contact your Faculty/Admin to Reset Device Binding.`,
          code: 'DEVICE_LOCKED',
          boundDeviceModel: student.device_model
        });
      }

      // If student has no device bound yet, bind this device now!
      if (!student.device_id && cleanDeviceId) {
        db.prepare(`
          UPDATE students 
          SET device_id = ?, device_model = ?, device_bound_at = CURRENT_TIMESTAMP 
          WHERE id = ?
        `).run(cleanDeviceId, cleanDeviceModel, student.id);
        console.log(`🔒 Initial Device Bound for ${student.roll_number}: ${cleanDeviceId} (${cleanDeviceModel})`);
      }

      // Update optional details
      if (cleanName || cleanEmail || cleanPhone) {
        db.prepare(`
          UPDATE students
          SET
            name = COALESCE(?, name),
            email = COALESCE(?, email),
            phone = COALESCE(?, phone)
          WHERE id = ?
        `).run(
          cleanName,
          cleanEmail,
          cleanPhone,
          student.id
        );
      }

      student = db.prepare(`
        SELECT * FROM students
        WHERE id = ?
      `).get(student.id);

      console.log(
        `🔐 Existing student verified: ${student.name} (${student.roll_number})`
      );
    }

    // --------------------------------------------------
    // 4. Count enrolled subjects
    // --------------------------------------------------
    const enrolledCount = db.prepare(`
      SELECT COUNT(*) as count
      FROM student_enrollments
      WHERE student_id = ?
    `).get(student.id).count;

    // --------------------------------------------------
    // 5. Generate JWT
    // --------------------------------------------------
    const token = jwt.sign(
      {
        id: student.id,
        name: student.name,
        rollNumber: student.roll_number,
        collegeId: student.college_id,
        semester: student.semester,
        department: student.department,
        role: 'STUDENT'
      },
      APP_CONFIG.JWT_SECRET,
      {
        expiresIn: APP_CONFIG.JWT_EXPIRES_IN
      }
    );

    // --------------------------------------------------
    // 6. Send response to Android app
    // --------------------------------------------------
    return res.status(200).json({
      success: true,
      message: 'Student registered/verified successfully.',
      token,

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
        deviceId: student.device_id,
        deviceModel: student.device_model,
        deviceBoundAt: student.device_bound_at,
        isDeviceBound: !!student.device_id,
        enrolledSubjectsCount: enrolledCount,
        verified: true
      }
    });

  } catch (error) {
    console.error('Error registering/verifying student:', error);

    return res.status(500).json({
      success: false,
      error: 'An internal server error occurred during student registration/verification.'
    });
  }
}

/**
 * Admin / Faculty Reset Device Binding for a Student
 * Sets device_id, device_model, device_bound_at to NULL so student can bind a new phone.
 */
export function resetStudentDeviceBinding(req, res) {
  try {
    const studentId = req.params.id;

    const student = db.prepare('SELECT * FROM students WHERE id = ?').get(studentId);
    if (!student) {
      return res.status(404).json({
        success: false,
        error: 'Student not found in database.'
      });
    }

    db.prepare(`
      UPDATE students 
      SET device_id = NULL, device_model = NULL, device_bound_at = NULL 
      WHERE id = ?
    `).run(studentId);

    console.log(`🔓 Device Binding Reset for student: ${student.name} (${student.roll_number})`);

    return res.status(200).json({
      success: true,
      message: `Device lock successfully reset for ${student.name} (${student.roll_number}). Student can now register and bind their new mobile device.`,
      studentId
    });
  } catch (error) {
    console.error('Reset device error:', error);
    return res.status(500).json({
      success: false,
      error: 'Failed to reset student device binding.'
    });
  }
}


/**
 * Teacher Registration
 */
export function registerTeacher(req, res) {
  try {
    const { name, email, password, department } = req.body;

    if (!name || !email || !password) {
      return res.status(400).json({
        success: false,
        error: 'Name, email, and password are required.'
      });
    }

    const cleanEmail = email.trim().toLowerCase();
    const existing = db.prepare('SELECT id FROM teachers WHERE email = ?').get(cleanEmail);
    if (existing) {
      return res.status(409).json({
        success: false,
        error: 'A teacher account with this email already exists.'
      });
    }

    const salt = bcrypt.genSaltSync(10);
    const passwordHash = bcrypt.hashSync(password, salt);
    const teacherId = 't_' + crypto.randomUUID();

    db.prepare(`
      INSERT INTO teachers (id, name, email, password_hash, department)
      VALUES (?, ?, ?, ?, ?)
    `).run(teacherId, name.trim(), cleanEmail, passwordHash, department || 'Computer Science & Engineering');

    const token = jwt.sign(
      {
        id: teacherId,
        name: name.trim(),
        email: cleanEmail,
        role: 'TEACHER'
      },
      APP_CONFIG.JWT_SECRET,
      { expiresIn: APP_CONFIG.JWT_EXPIRES_IN }
    );

    return res.status(201).json({
      success: true,
      message: 'Teacher account registered successfully.',
      token,
      teacher: { id: teacherId, name: name.trim(), email: cleanEmail, department }
    });
  } catch (error) {
    console.error('Teacher registration error:', error);
    return res.status(500).json({
      success: false,
      error: 'Failed to create teacher account.'
    });
  }
}

/**
 * Teacher Login
 */
export function loginTeacher(req, res) {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({
        success: false,
        error: 'Email and password are required.'
      });
    }

    const cleanEmail = email.trim().toLowerCase();
    const teacher = db.prepare('SELECT * FROM teachers WHERE email = ?').get(cleanEmail);

    if (!teacher || !bcrypt.compareSync(password, teacher.password_hash)) {
      return res.status(401).json({
        success: false,
        error: 'Invalid email or password.'
      });
    }

    const token = jwt.sign(
      {
        id: teacher.id,
        name: teacher.name,
        email: teacher.email,
        department: teacher.department,
        role: teacher.role || 'TEACHER'
      },
      APP_CONFIG.JWT_SECRET,
      { expiresIn: APP_CONFIG.JWT_EXPIRES_IN }
    );

    return res.status(200).json({
      success: true,
      message: 'Logged in successfully.',
      token,
      teacher: {
        id: teacher.id,
        name: teacher.name,
        email: teacher.email,
        department: teacher.department,
        role: teacher.role
      }
    });
  } catch (error) {
    console.error('Teacher login error:', error);
    return res.status(500).json({
      success: false,
      error: 'Teacher login failed due to server error.'
    });
  }
}
