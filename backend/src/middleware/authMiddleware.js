import jwt from 'jsonwebtoken';
import { APP_CONFIG } from '../config/constants.js';

export function authenticateStudent(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({
      success: false,
      error: 'Authentication token required'
    });
  }

  const token = authHeader.split(' ')[1];
  try {
    const decoded = jwt.verify(token, APP_CONFIG.JWT_SECRET);
    if (decoded.role !== 'STUDENT') {
      return res.status(403).json({
        success: false,
        error: 'Unauthorized: Student access required'
      });
    }
    req.user = decoded;
    next();
  } catch (err) {
    return res.status(401).json({
      success: false,
      error: 'Session expired or invalid. Please re-verify your student identity.'
    });
  }
}

export function authenticateTeacher(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({
      success: false,
      error: 'Teacher authentication token required'
    });
  }

  const token = authHeader.split(' ')[1];
  try {
    const decoded = jwt.verify(token, APP_CONFIG.JWT_SECRET);
    if (decoded.role !== 'TEACHER' && decoded.role !== 'ADMIN') {
      return res.status(403).json({
        success: false,
        error: 'Unauthorized: Teacher or Admin access required'
      });
    }
    req.user = decoded;
    next();
  } catch (err) {
    return res.status(401).json({
      success: false,
      error: 'Teacher session expired. Please log in again.'
    });
  }
}
