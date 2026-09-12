import crypto from 'crypto';
import { APP_CONFIG } from '../config/constants.js';

/**
 * Creates a cryptographically signed dynamic QR token for an active class session.
 * 
 * @param {string} sessionId 
 * @param {number} validitySeconds 
 * @returns {{ token: string, expiresAt: string, payload: object }}
 */
export function generateSignedQrToken(sessionId, validitySeconds = APP_CONFIG.DEFAULT_QR_VALIDITY_SECONDS) {
  const issuedAt = Math.floor(Date.now() / 1000);
  const expiresAt = issuedAt + validitySeconds;
  const nonce = crypto.randomBytes(6).toString('hex');

  const payload = {
    sessionId,
    iat: issuedAt,
    exp: expiresAt,
    nonce
  };

  const payloadString = JSON.stringify(payload);
  const payloadBase64 = Buffer.from(payloadString).toString('base64url');

  const signature = crypto
    .createHmac('sha256', APP_CONFIG.QR_SIGNING_SECRET)
    .update(payloadBase64)
    .digest('base64url');

  const token = `${payloadBase64}.${signature}`;

  return {
    token,
    expiresAt: new Date(expiresAt * 1000).toISOString(),
    payload
  };
}

/**
 * Validates a signed QR token string.
 * 
 * @param {string} token 
 * @returns {{ valid: boolean, error?: string, payload?: object }}
 */
export function verifySignedQrToken(token) {
  if (!token || typeof token !== 'string') {
    return { valid: false, error: 'Malformed QR code token' };
  }

  const parts = token.split('.');
  if (parts.length !== 2) {
    return { valid: false, error: 'Invalid QR token structure' };
  }

  const [payloadBase64, signature] = parts;

  // Verify HMAC signature
  const expectedSignature = crypto
    .createHmac('sha256', APP_CONFIG.QR_SIGNING_SECRET)
    .update(payloadBase64)
    .digest('base64url');

  if (!crypto.timingSafeEqual(Buffer.from(signature), Buffer.from(expectedSignature))) {
    return { valid: false, error: 'Forged or tampered QR Code' };
  }

  try {
    const payload = JSON.parse(Buffer.from(payloadBase64, 'base64url').toString('utf8'));
    const nowSeconds = Math.floor(Date.now() / 1000);

    if (nowSeconds > payload.exp) {
      return { 
        valid: false, 
        error: 'QR Code Expired. Please scan the refreshed QR code on the teacher screen.', 
        expired: true,
        payload 
      };
    }

    return { valid: true, payload };
  } catch (err) {
    return { valid: false, error: 'Corrupt QR code payload' };
  }
}
