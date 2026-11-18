import test from 'node:test';
import assert from 'node:assert/strict';
import { signCloudToken } from '../cloudflare/shared/cloud-token.js';
import cloudWorker from '../cloudflare/appforge-cloud-sync/worker/src/index.js';

const SECRET='cloud-test-secret-with-more-than-thirty-two-bytes';
const id1='123e4567-e89b-12d3-a456-426614174000';
const id2='123e4567-e89b-12d3-a456-426614174001';
const installA='a'.repeat(64),installB='b'.repeat(64);
function mockEnv({revoked=[],devices=[installA,installB],limit=100}={}) {
  const licenses=new Map([[id1,{status:'active',paid_confirmed:1,expires_at:null,cloud_enabled:1,suspended_at:null,cloud_daily_limit:limit}],[id2,{status:'active',paid_confirmed:1,expires_at:null,cloud_enabled:1,suspended_at:null,cloud_daily_limit:limit}]]);
  for(const id of revoked)licenses.get(id).status='revoked';
  const activations=new Set([[id1,installA],[id1,installB],[id2,installA]].map(x=>x.join(':')));
  for(const d of devices) if(d!==installA&&d!==installB)activations.add(id1+':'+d);
  const snapshots=new Map(),usage=new Map();
  const DB={prepare(sql){return{bind(...a){return{
    first:async()=>{
      if(sql.includes('INSERT INTO license_sync_usage')){const key=a[0]+':'+a[1],current=usage.get(key)?.requests||0,cap=a[2];if(current>=cap)return null;const row={requests:current+1};usage.set(key,row);return row}
      if(sql.includes('FROM license_sync_usage WHERE'))return usage.get(a[0]+':'+a[1])||null;
      if(sql.includes('FROM license_snapshots WHERE')){const v=snapshots.get(a[0]+':'+a[1]);return v?{revision:v.revision,payload:JSON.stringify(v.snapshot),updatedAt:'2026-01-01T00:00:00.000Z',updatedByInstall:v.installHash}:null}
      return null
    },
    run:async()=>{
      if(sql.startsWith('UPDATE license_snapshots')){const key=a[3]+':'+a[4],v=snapshots.get(key);if(!v||v.revision!==a[5])return{meta:{changes:0}};snapshots.set(key,{revision:a[1],snapshot:JSON.parse(a[0]),installHash:a[2]});return{meta:{changes:1}}}
      if(sql.startsWith('INSERT INTO license_snapshots')){const key=a[0]+':'+a[1];if(snapshots.has(key))throw Error('unique');snapshots.set(key,{revision:1,snapshot:JSON.parse(a[2]),installHash:a[3]});return{meta:{changes:1}}}
      return{meta:{changes:1}}
    }
  }}}}};
  const LICENSES={prepare(sql){return{bind(...a){return{first:async()=>{
    if(sql.includes('FROM licenses WHERE'))return licenses.get(a[0])||null;
    if(sql.includes('FROM activations WHERE'))return activations.has(a[0]+':'+a[1])?{id:1}:null;
    return null
  }}}}}};
  return{env:{DB,LICENSES,CLOUD_TOKEN_HMAC_SECRET:SECRET,APP_ORIGINS:'https://localhost'},snapshots};
}
async function bearer(licenseId=id1,installHash=installA,kind='license',time=Date.now()){
 return signCloudToken(SECRET,{scope:'lifeos:snapshot',productId:'LIFEOS-V1',licenseId,installHash,kind},time);
}
async function call(env,method,token,body){
 const headers={Authorization:'Bearer '+token,Origin:'https://localhost'};
 if(body!==undefined)headers['Content-Type']='application/json';
 const response=await cloudWorker.fetch(new Request('https://cloud.test/v1/lifeos/snapshot',{method,headers,body:body===undefined?undefined:JSON.stringify(body)}),env);
 return{response,data:await response.json()};
}

test('snapshots are isolated per license and shared with devices on that license',async()=>{
 const {env}=mockEnv();
 const t1=await bearer(),secondDevice=await bearer(id1,installB),t2=await bearer(id2,installA);
 let r=await call(env,'GET',t1);assert.equal(r.data.revision,0);assert.equal(r.data.snapshot,null);
 r=await call(env,'PUT',t1,{expectedRevision:0,snapshot:{goals:['g1'],habits:['h1']}});assert.equal(r.response.status,200);assert.equal(r.data.revision,1);
 r=await call(env,'GET',secondDevice);assert.equal(r.data.snapshot.goals[0],'g1');
 r=await call(env,'GET',t2);assert.equal(r.data.snapshot,null);
});

test('stale revisions conflict and revoked or unapproved devices are refused',async()=>{
 const {env}=mockEnv({revoked:[id2]});
 const t1=await bearer();
 await call(env,'PUT',t1,{expectedRevision:0,snapshot:{goals:['local']}});
 let r=await call(env,'PUT',t1,{expectedRevision:0,snapshot:{goals:['duplicate']}});
 assert.equal(r.response.status,409);
 r=await call(env,'GET',await bearer(id2));assert.equal(r.response.status,403);
 r=await call(env,'GET',await bearer(id1,'c'.repeat(64)));assert.equal(r.response.status,403);
});

test('per-license quota blocks requests without mutating snapshots',async()=>{
 const {env}=mockEnv({limit:1}),token=await bearer();
 assert.equal((await call(env,'GET',token)).response.status,200);
 assert.equal((await call(env,'GET',token)).response.status,429);
});
