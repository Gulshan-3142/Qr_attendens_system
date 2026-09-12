import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import path from 'path';
import { fileURLToPath } from 'url';
import { APP_CONFIG } from './config/constants.js';
import db, { initDatabase } from './config/db.js';
import { runSeed } from './seed/seedData.js';
import { errorHandler } from './middleware/errorHandler.js';
import { authenticateStudent, authenticateTeacher } from './middleware/authMiddleware.js';

// Controllers
import { verifyStudent, registerTeacher, loginTeacher, resetStudentDeviceBinding } from './controllers/authController.js';
import { scanAttendance } from './controllers/attendanceController.js';
import {
  createSession,
  refreshSessionQr,
  getLiveSessionAttendance,
  closeSession,
  toggleManualAttendance,
  sessionSseStream
} from './controllers/sessionController.js';
import {
  getStudentProfile,
  getAttendanceSummary,
  getSubjectWiseAttendance,
  getAttendanceHistory,
  getAttendanceTrends
} from './controllers/studentController.js';
import {
  calculateLeaveDays,
  calculateSkipClasses,
  calculateTargetAttendance,
  calculateAttendancePlanner,
  calculateHolidayImpact
} from './controllers/calculatorController.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();

// Initialize DB schema & seed if needed
initDatabase();
const studentCount = db.prepare('SELECT COUNT(*) as count FROM students').get().count;
if (studentCount === 0) {
  runSeed();
}

// Global Middlewares
app.use(helmet({
  contentSecurityPolicy: false // Allow inline scripts and CDNs for teacher portal
}));
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// for temprary
app.use((req, res, next) => {
  console.log(`📥 ${req.method} ${req.url}`);
  next();
});
// Serve Teacher & Admin Web Portal
app.use(express.static(path.join(__dirname, 'public')));

// ----------------------------------------------------
// REST API ROUTES
// ----------------------------------------------------

// 1. Authentication Endpoints
app.post('/api/auth/student/verify', verifyStudent);
app.post('/api/auth/teacher/register', registerTeacher);
app.post('/api/auth/teacher/login', loginTeacher);

// 2. Attendance Scanning (Student App)
app.post('/api/attendance/scan', scanAttendance);

// 3. Student Attendance Analytics & Profile
app.get('/api/student/profile', getStudentProfile);
app.get('/api/student/attendance/summary', getAttendanceSummary);
app.get('/api/student/attendance/subjects', getSubjectWiseAttendance);
app.get('/api/student/attendance/history', getAttendanceHistory);
app.get('/api/student/analytics/trends', getAttendanceTrends);

// 4. Calculators & Day Planner
app.post('/api/student/calculators/leave', calculateLeaveDays);
app.post('/api/student/calculators/skip', calculateSkipClasses);
app.post('/api/student/calculators/target', calculateTargetAttendance);
app.post('/api/student/calculators/planner', calculateAttendancePlanner);
app.post('/api/student/calculators/holiday-impact', calculateHolidayImpact);

// 5. Teacher Session & Live Management
app.post('/api/teacher/sessions/create', authenticateTeacher, createSession);
app.get('/api/config', (req, res) => {
  res.json({
    success: true,
    defaultQrValiditySeconds: APP_CONFIG.DEFAULT_QR_VALIDITY_SECONDS,
    defaultGeofenceRadiusMeters: APP_CONFIG.DEFAULT_GEOFENCE_RADIUS_METERS
  });
});

app.get('/api/teacher/sessions/active', (req, res) => {
  const session = db.prepare(`
    SELECT cs.*, s.name as subject_name, s.code as subject_code
    FROM class_sessions cs
    JOIN subjects s ON cs.subject_id = s.id
    WHERE cs.status = 'ACTIVE'
    ORDER BY cs.created_at DESC
    LIMIT 1
  `).get();
  if (session) {
    session.qrValiditySeconds = APP_CONFIG.DEFAULT_QR_VALIDITY_SECONDS;
  }
  return res.status(200).json({ 
    success: true, 
    session: session || null,
    defaultValiditySeconds: APP_CONFIG.DEFAULT_QR_VALIDITY_SECONDS
  });
});
app.post('/api/teacher/sessions/:id/qr', refreshSessionQr);
app.get('/api/teacher/sessions/:id/live-attendance', getLiveSessionAttendance);
app.post('/api/teacher/sessions/:id/close', closeSession);
app.post('/api/teacher/sessions/:id/manual-attendance', toggleManualAttendance);
app.post('/api/teacher/students/:id/reset-device', resetStudentDeviceBinding);
app.get('/api/teacher/sessions/:id/stream', sessionSseStream);

// Health check endpoint
app.get('/api/health', (req, res) => {
  res.json({
    status: 'healthy',
    timestamp: new Date().toISOString(),
    database: 'SQLite (WAL mode)',
    version: '1.0.0'
  });
});

// Centralized error handler
app.use(errorHandler);

const PORT = process.env.PORT || APP_CONFIG.PORT;
if (process.env.NODE_ENV !== 'test') {
  app.listen(PORT, '0.0.0.0', () => {
    console.log(`=======================================================`);
    console.log(`🚀 SmartQR Attendance Backend Server running!`);
    console.log(`📡 REST API: http://localhost:${PORT}/api`);
    console.log(`💻 Teacher/Admin Portal: http://localhost:${PORT}`);
    console.log(`=======================================================`);
  });
}

export default app;
