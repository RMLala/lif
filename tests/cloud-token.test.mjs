import test from 'node:test';
import assert from 'node:assert/strict';
import { signCloudToken, verifyCloudToken } from '../cloudflare/shared/cloud-token.js';

const secret = 'test-secret-with-at-least-thirty-two-bytes';
const claims = { scope: 'lifeos:snapshot', productId: 'LIFEOS-V1', licenseId: '123e4567-e89b-12d3-a456-426614174000', installHash: 'a'.repeat(64), kind: 'license' };

test('Cloud grant is signed, scoped and valid for its short lifetime', async () => {
  const now = 1_800_000_000_000;
  const token = await signCloudToken(secret, claims, now);
  const decoded = await verifyCloudToken(secret, token, { licenseId: claims.licenseId, installHash: claims.installHash }, now + 30_000);
  assert.equal(decoded.scope, claims.scope);
  assert.equal(decoded.exp - decoded.iat, 600);
});

test('Cloud grant rejects a changed signature and a different tenant', async () => {
  const token = await signCloudToken(secret, claims);
  const [payload, signature] = token.split('.');
  const altered = payload + '.' + (signature[0] === 'A' ? 'B' : 'A') + signature.slice(1);
  await assert.rejects(() => verifyCloudToken(secret, altered), /invalid_token/);
  await assert.rejects(() => verifyCloudToken(secret, token, { licenseId: '123e4567-e89b-12d3-a456-426614174001' }), /invalid_scope/);
});

test('Cloud grant is rejected after expiry and if the signing secret is absent', async () => {
  const now = 1_800_000_000_000;
  const token = await signCloudToken(secret, claims, now);
  await assert.rejects(() => verifyCloudToken(secret, token, {}, now + 601_000), /expired_token/);
  await assert.rejects(() => verifyCloudToken('short', token), /Cloud token secret/);
});
