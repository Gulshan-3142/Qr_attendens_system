export const APP_CONFIG = {
  PORT: process.env.PORT || 3000,
  JWT_SECRET: process.env.JWT_SECRET || 'qr_attendance_super_secret_jwt_key_2026',
  JWT_EXPIRES_IN: '30d',
  
  // Geofencing Defaults
  DEFAULT_GEOFENCE_RADIUS_METERS: 15, // 15-20 meters default
  MAX_ALLOWED_GPS_ACCURACY_METERS: 100, // Reject if device accuracy is worse than 25m
  
  // QR Code Expiry Settings
  DEFAULT_QR_VALIDITY_SECONDS: 10, // 10 seconds rolling validity
  QR_SIGNING_SECRET: process.env.QR_SIGNING_SECRET || 'qr_hmac_sign_key_99218274_prod',
  
  // Campus Baseline Coordinates (Default: Computer Science & Engineering Building)
  CAMPUS_DEFAULT_LAT: 28.6139,
  CAMPUS_DEFAULT_LNG: 77.2090
};
