import Database from 'better-sqlite3';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const dbPath = path.resolve(__dirname, '../../attendance.db');
const db = new Database(dbPath);

// Enable WAL mode for high concurrency and performance
db.pragma('journal_mode = WAL');
db.pragma('foreign_keys = ON');

export function initDatabase() {
  db.exec(`
    CREATE TABLE IF NOT EXISTS students (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      roll_number TEXT UNIQUE NOT NULL,
      college_id TEXT UNIQUE NOT NULL,
      email TEXT,
      phone TEXT,
      avatar_url TEXT,
      department TEXT DEFAULT 'Computer Science & Engineering',
      semester INTEGER DEFAULT 6,
      device_id TEXT DEFAULT NULL,
      device_model TEXT DEFAULT NULL,
      device_bound_at DATETIME DEFAULT NULL,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS teachers (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      email TEXT UNIQUE NOT NULL,
      password_hash TEXT NOT NULL,
      department TEXT DEFAULT 'Computer Science & Engineering',
      role TEXT DEFAULT 'TEACHER',
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS subjects (
      id TEXT PRIMARY KEY,
      code TEXT UNIQUE NOT NULL,
      name TEXT NOT NULL,
      teacher_id TEXT,
      semester INTEGER NOT NULL,
      credits INTEGER DEFAULT 4,
      department TEXT DEFAULT 'Computer Science & Engineering',
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE SET NULL
    );

    CREATE TABLE IF NOT EXISTS student_enrollments (
      id TEXT PRIMARY KEY,
      student_id TEXT NOT NULL,
      subject_id TEXT NOT NULL,
      semester INTEGER NOT NULL,
      enrolled_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
      FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
      UNIQUE (student_id, subject_id)
    );

    CREATE TABLE IF NOT EXISTS class_sessions (
      id TEXT PRIMARY KEY,
      subject_id TEXT NOT NULL,
      teacher_id TEXT NOT NULL,
      room_name TEXT NOT NULL,
      date TEXT NOT NULL,
      start_time TEXT NOT NULL,
      end_time TEXT NOT NULL,
      latitude REAL NOT NULL,
      longitude REAL NOT NULL,
      allowed_radius_meters REAL DEFAULT 20.0,
      qr_token TEXT,
      qr_expires_at DATETIME,
      status TEXT DEFAULT 'ACTIVE',
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
      FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS attendance_records (
      id TEXT PRIMARY KEY,
      student_id TEXT NOT NULL,
      session_id TEXT NOT NULL,
      subject_id TEXT NOT NULL,
      status TEXT NOT NULL CHECK(status IN ('PRESENT', 'ABSENT', 'LATE')),
      verification_method TEXT DEFAULT 'QR_GEOFENCE',
      location_verified INTEGER DEFAULT 1,
      distance_meters REAL,
      student_lat REAL,
      student_lng REAL,
      gps_accuracy REAL,
      timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
      FOREIGN KEY (session_id) REFERENCES class_sessions(id) ON DELETE CASCADE,
      FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
      UNIQUE (student_id, session_id)
    );

    CREATE TABLE IF NOT EXISTS academic_calendar (
      id TEXT PRIMARY KEY,
      date TEXT UNIQUE NOT NULL,
      is_academic_day INTEGER DEFAULT 1,
      is_holiday INTEGER DEFAULT 0,
      holiday_name TEXT,
      day_of_week TEXT
    );

    -- Indexes for high-performance lookups
    CREATE INDEX IF NOT EXISTS idx_students_roll ON students(roll_number);
    CREATE INDEX IF NOT EXISTS idx_students_college ON students(college_id);
    CREATE INDEX IF NOT EXISTS idx_attendance_student ON attendance_records(student_id);
    CREATE INDEX IF NOT EXISTS idx_attendance_session ON attendance_records(session_id);
    CREATE INDEX IF NOT EXISTS idx_attendance_subject ON attendance_records(subject_id);
    CREATE INDEX IF NOT EXISTS idx_attendance_timestamp ON attendance_records(timestamp);
    CREATE INDEX IF NOT EXISTS idx_sessions_active ON class_sessions(status, qr_expires_at);
  `);

  // Safe migration for existing databases
  try {
    db.exec(`ALTER TABLE students ADD COLUMN device_id TEXT DEFAULT NULL;`);
  } catch (e) {}
  try {
    db.exec(`ALTER TABLE students ADD COLUMN device_model TEXT DEFAULT NULL;`);
  } catch (e) {}
  try {
    db.exec(`ALTER TABLE students ADD COLUMN device_bound_at DATETIME DEFAULT NULL;`);
  } catch (e) {}
  try {
    db.exec(`CREATE UNIQUE INDEX IF NOT EXISTS idx_students_device_unique ON students(device_id) WHERE device_id IS NOT NULL;`);
  } catch (e) {}
}

export default db;
