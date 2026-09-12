/**
 * Geofencing and geodesic distance utility functions
 */

const EARTH_RADIUS_METERS = 6371000; // 6,371 km in meters

/**
 * Calculates the great-circle distance between two points on the Earth's surface
 * using the Haversine formula.
 * 
 * @param {number} lat1 Latitude of point 1 (degrees)
 * @param {number} lon1 Longitude of point 1 (degrees)
 * @param {number} lat2 Latitude of point 2 (degrees)
 * @param {number} lon2 Longitude of point 2 (degrees)
 * @returns {number} Distance in meters
 */
export function calculateHaversineDistance(lat1, lon1, lat2, lon2) {
  const toRad = (degrees) => (degrees * Math.PI) / 180;

  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);

  const phi1 = toRad(lat1);
  const phi2 = toRad(lat2);

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(phi1) * Math.cos(phi2);

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

  const distance = EARTH_RADIUS_METERS * c;
  return Math.round(distance * 10) / 10; // Round to 1 decimal place (e.g. 8.4 meters)
}

/**
 * Verifies if student coordinates fall within the authorized session radius.
 * 
 * @param {number} studentLat 
 * @param {number} studentLng 
 * @param {number} sessionLat 
 * @param {number} sessionLng 
 * @param {number} allowedRadiusMeters 
 * @returns {{ isWithinGeofence: boolean, distanceMeters: number }}
 */
export function verifyGeofence(studentLat, studentLng, sessionLat, sessionLng, allowedRadiusMeters) {
  const distance = calculateHaversineDistance(studentLat, studentLng, sessionLat, sessionLng);
  return {
    isWithinGeofence: distance <= allowedRadiusMeters,
    distanceMeters: distance,
    allowedRadiusMeters: allowedRadiusMeters
  };
}
