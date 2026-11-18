const encoder = new TextEncoder();
const b64url = bytes => btoa(String.fromCharCode(...new Uint8Array(bytes))).replace(/\+/g,"-").replace(/\//g,"_").replace(/=+$/g,"");
const fromB64url = value => Uint8Array.from(atob(value.replace(/-/g,"+").replace(/_/g,"/")+"===".slice((value.length+3)%4)), char => char.charCodeAt(0));
async function hmacKey(secret, usages) {
  if (typeof secret !== "string" || secret.length < 32) throw new Error("Cloud token secret is not configured.");
  return crypto.subtle.importKey("raw", encoder.encode(secret), { name: "HMAC", hash: "SHA-256" }, false, usages);
}
export async function signCloudToken(secret, claims, now = Date.now()) {
  const issuedAt = Math.floor(now / 1000);
  const payload = { ...claims, iss: "appforge-license", aud: "appforge-cloud", iat: issuedAt, nbf: issuedAt, exp: issuedAt + 600 };
  const encoded = b64url(encoder.encode(JSON.stringify(payload)));
  const signature = await crypto.subtle.sign("HMAC", await hmacKey(secret, ["sign"]), encoder.encode(encoded));
  return encoded + "." + b64url(signature);
}
export async function verifyCloudToken(secret, token, expected = {}, now = Date.now()) {
  if (typeof token !== "string" || token.length > 4096) throw new Error("invalid_token");
  const parts = token.split(".");
  if (parts.length !== 2) throw new Error("invalid_token");
  let claims;
  try {
    claims = JSON.parse(new TextDecoder().decode(fromB64url(parts[0])));
  } catch {
    throw new Error("invalid_token");
  }
  const valid = await crypto.subtle.verify("HMAC", await hmacKey(secret, ["verify"]), fromB64url(parts[1]), encoder.encode(parts[0]));
  if (!valid || claims.iss !== "appforge-license" || claims.aud !== "appforge-cloud") throw new Error("invalid_token");
  const nowSeconds = Math.floor(now / 1000);
  if (!Number.isSafeInteger(claims.iat) || !Number.isSafeInteger(claims.exp) || claims.iat > nowSeconds + 30 || claims.nbf > nowSeconds + 30 || claims.exp <= nowSeconds) throw new Error("expired_token");
  for (const [key, value] of Object.entries(expected)) if (claims[key] !== value) throw new Error("invalid_scope");
  if (claims.scope !== "lifeos:snapshot") throw new Error("invalid_scope");
  if (!/^[0-9a-f-]{36}$/i.test(String(claims.licenseId || "")) || !/^[a-f0-9]{64}$/i.test(String(claims.installHash || "")) || claims.productId !== "LIFEOS-V1") throw new Error("invalid_scope");
  return claims;
}
