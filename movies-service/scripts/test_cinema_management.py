#!/usr/bin/env python3
"""Run cinema API checks through Postman CLI; reuse named fixtures, never delete data."""
from pathlib import Path
from datetime import datetime, timedelta, time
from zoneinfo import ZoneInfo
import json, subprocess, sys

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/testing/cinema-management'
OUT.mkdir(parents=True, exist_ok=True)
BASE = 'http://localhost:8082'
MOVIES = [('Inception', '6ae871d6a6456bb77994130a'), ('Guardians of the Galaxy Vol. 2', '6ae8769ca6456bb77994130b')]
MISSING = '000000000000000000000000'
ROWS = []
FIXTURES = {'theater': None, 'screen': None, 'seats': [], 'showtimes': [], 'created': []}
STAMP = datetime.now(ZoneInfo('Asia/Bahrain')).isoformat()

def save():
    report = {'startedAt': STAMP, 'baseUrl': BASE, 'rows': ROWS, 'fixtures': FIXTURES,
              'assertions': {k: sum(r.get('tests', {}).get(k, 0) for r in ROWS) for k in ['total', 'passed', 'failed']}}
    (OUT / 'report.json').write_text(json.dumps(report, indent=2) + '\n')

def call(name, method, path, expected=200, body=None, check='', headers=None, critical=True):
    n = len(ROWS) + 1
    output = OUT / f'{n:02d}-response.json'
    assertions = f'pm.test("HTTP {expected}", () => pm.response.to.have.status({expected}));\n' + check
    script = OUT / f'{n:02d}-assertions.js'; script.write_text(assertions + '\n')
    cmd = ['postman', 'request', method, BASE + path, '--no-report-events', '--timeout', '15000',
           '--output', str(output), '--script-post-request', '@' + str(script)]
    if body is not None:
        payload = OUT / f'{n:02d}-request.json'; payload.write_text(json.dumps(body, indent=2) + '\n')
        cmd += ['-H', 'Content-Type:application/json', '--body', json.dumps(body)]
    for key, value in (headers or {}).items(): cmd += ['-H', key + ':' + value]
    result = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, timeout=45)
    (OUT / f'{n:02d}-postman.log').write_text(result.stdout)
    raw = json.loads(output.read_text()) if output.exists() else {}
    status = raw.get('response', {}).get('code')
    tests = raw.get('tests', {})
    # CLI exits nonzero for expected 4xx responses even when every assertion passes.
    passed = status == expected and tests.get('total', 0) > 0 and tests.get('failed', 1) == 0
    row = {'feature': name, 'method': method, 'endpoint': path, 'expected': expected, 'actual': status,
           'result': 'PASS' if passed else 'FAIL', 'tests': tests, 'exitCode': result.returncode,
           'evidence': output.name}
    ROWS.append(row); save()
    print(f'{row["result"]}: {name}: expected {expected}, actual {status}', flush=True)
    data = json.loads(raw['response']['body']) if raw.get('response', {}).get('body') else None
    if not passed and critical: raise RuntimeError(f'{name} failed: {data}; see {output}')
    return data

def expect_id(id):
    return 'pm.test("Expected resource ID", () => pm.expect(pm.response.json().id).to.eql(' + json.dumps(id) + '));'

def expect_member(id, fields=None):
    return ('pm.test("Contains expected persisted resource", () => { const r = pm.response.json().find(x => x.id === '
            + json.dumps(id) + '); pm.expect(r).to.exist; '
            + ''.join('pm.expect(r[' + json.dumps(k) + ']).to.eql(' + json.dumps(v) + ');' for k,v in (fields or {}).items()) + '});')

def main():
    for title, id in MOVIES:
        call('Existing movie: ' + title, 'GET', '/api/movies/' + id,
             check=expect_id(id) + 'pm.test("Movie is showing", () => pm.expect(pm.response.json().isShowing).to.eql(true));')
    theaters = call('List theaters / discover reusable fixture', 'GET', '/api/theaters')
    theater = next((t for t in theaters if t['name'] == 'CinemaSaar Saar - API Test' and t['location'] == 'Saar, Northern Governorate, Bahrain'), None)
    if theater is None:
        theater = call('Create theater', 'POST', '/api/theaters', 201,
                       {'name': 'CinemaSaar Saar - API Test', 'location': 'Saar, Northern Governorate, Bahrain'},
                       'pm.test("Theater has an ID", () => pm.expect(pm.response.json().id).to.be.a("string"));')
        FIXTURES['created'].append({'type': 'theater', 'id': theater['id']})
    FIXTURES['theater'] = theater; save(); tid = theater['id']
    call('Retrieve theater by ID', 'GET', '/api/theaters/' + tid, check=expect_id(tid))
    screens = call('List screens by theater', 'GET', '/api/screens/theater/' + tid)
    screen = next((s for s in screens if s['name'] == 'Screen 1 - API Test'), None)
    if screen is None:
        screen = call('Create screen in theater', 'POST', '/api/screens', 201, {'theaterId': tid, 'name': 'Screen 1 - API Test'})
        FIXTURES['created'].append({'type': 'screen', 'id': screen['id']})
    FIXTURES['screen'] = screen; save(); sid = screen['id']
    call('Retrieve screen / verify theater reference', 'GET', '/api/screens/' + sid,
         check=expect_id(sid) + 'pm.test("Screen belongs to theater", () => pm.expect(pm.response.json().theaterId).to.eql(' + json.dumps(tid) + '));')
    call('Retrieve all screens', 'GET', '/api/screens', check=expect_member(sid, {'theaterId': tid}))
    seats = call('Inspect existing physical seats', 'GET', '/api/seats/screen/' + sid)
    if not seats:
        seats = call('Generate 30 physical seats', 'POST', '/api/seats/generate', 201,
                     {'screenId': sid, 'rows': ['A', 'B', 'C'], 'seatsPerRow': 10},
                     'pm.test("30 generated seats", () => pm.expect(pm.response.json()).to.have.length(30));')
        FIXTURES['created'] += [{'type': 'seat', 'id': s['id']} for s in seats]
    FIXTURES['seats'] = seats; save()
    call('Retrieve seats / verify unique physical definitions', 'GET', '/api/seats/screen/' + sid,
         check='pm.test("Three rows of ten unique seats", () => { const s=pm.response.json(); pm.expect(s).to.have.length(30); '
               'pm.expect(new Set(s.map(x=>x.rowLabel+":"+x.seatNumber)).size).to.eql(30); '
               'pm.expect(s.every(x=>x.screenId===' + json.dumps(sid) + ')).to.eql(true); });')
    scheduled = call('Inspect screen schedule before validation', 'GET', '/api/showtimes/screen/' + sid)
    if not scheduled:
        call('Reject duplicate A1 seat (case normalized)', 'POST', '/api/seats', 409,
             {'screenId': sid, 'rowLabel': 'a', 'seatNumber': 1},
             'pm.test("Duplicate detected", () => pm.expect(pm.response.json().message).to.eql("Seat already exists"));', critical=False)
        call('Reject duplicate seat-generation rows', 'POST', '/api/seats/generate', 400,
             {'screenId': sid, 'rows': ['D', 'd'], 'seatsPerRow': 10}, critical=False)
        call('Reject duplicate seat generation', 'POST', '/api/seats/generate', 409,
             {'screenId': sid, 'rows': ['A'], 'seatsPerRow': 10}, critical=False)
    else:
        print('Existing schedule: duplicate-seat checks are masked by immutable layout; no duplicate fixtures created.', flush=True)
    call('Reject screen with missing theater', 'POST', '/api/screens', 404, {'theaterId': MISSING, 'name': 'Invalid reference'}, critical=False)
    call('Reject seat with missing screen', 'POST', '/api/seats', 404,
         {'screenId': MISSING, 'rowLabel': 'A', 'seatNumber': 1}, critical=False)
    call('Reject seat number zero', 'POST', '/api/seats', 400,
         {'screenId': sid, 'rowLabel': 'A', 'seatNumber': 0}, critical=False)
    call('Reject blank theater name', 'POST', '/api/theaters', 400, {'name': ' ', 'location': 'Saar'}, critical=False)
    # Reuse the original dates on reruns; otherwise use tomorrow in Bahrain.
    tomorrow = datetime.now(ZoneInfo('Asia/Bahrain')).date() + timedelta(days=1)
    first = datetime.combine(tomorrow, time(18), ZoneInfo('Asia/Bahrain'))
    for i, (title, mid) in enumerate(MOVIES):
        existing = next((x for x in scheduled if x['movieId'] == mid and x['status']=='SCHEDULED'
                         and datetime.fromisoformat(x['startsAt'].replace('Z','+00:00')) > datetime.now(ZoneInfo('Asia/Bahrain'))), None)
        start = first + timedelta(hours=3*i); end = start + timedelta(minutes=160 if i==0 else 150)
        body = {'movieId':mid,'screenId':sid,'startsAt':start.isoformat(),'endsAt':end.isoformat(),'basePrice':4.5}
        if existing is None:
            # Move the test pair to the next free day if unrelated screenings occupy these intervals.
            while any(x['status']!='CANCELLED' and datetime.fromisoformat(x['startsAt'].replace('Z','+00:00')) < end
                      and datetime.fromisoformat(x['endsAt'].replace('Z','+00:00')) > start for x in scheduled):
                start += timedelta(days=1);end += timedelta(days=1)
            body.update(startsAt=start.isoformat(),endsAt=end.isoformat())
            showtime = call('Create showtime: ' + title,'POST','/api/showtimes',201,body,
                           headers={'X-User-Id':'cinemasaar-api-test-admin'})
            FIXTURES['created'].append({'type':'showtime','id':showtime['id']})
            scheduled.append(showtime)
        else:showtime=existing
        FIXTURES['showtimes'].append(showtime);save()
        call('Retrieve showtime: ' + title,'GET','/api/showtimes/'+showtime['id'],check=expect_id(showtime['id']))
        call('Verify showtime movie relationship: ' + title,'GET','/api/showtimes/movie/'+mid,
             check=expect_member(showtime['id'],{'movieId':mid,'screenId':sid,'status':'SCHEDULED'}))
    first_show=FIXTURES['showtimes'][0]
    basebody={k:first_show[k] for k in ['movieId','screenId','startsAt','endsAt','basePrice']}
    call('Reject overlapping showtime','POST','/api/showtimes',409,basebody,
         'pm.test("Overlap message", () => pm.expect(pm.response.json().message).to.eql("Screen already has an overlapping showtime"));',
         headers={'X-User-Id':'cinemasaar-api-test-admin'},critical=False)
    for feature,change,code in [('Reject missing movie reference',{'movieId':MISSING},404),
                                ('Reject missing screen reference',{'screenId':MISSING},404),
                                ('Reject equal start/end',{'endsAt':basebody['startsAt']},400),
                                ('Reject negative price',{'basePrice':-1},400),
                                ('Reject past showtime',{'startsAt':'2020-01-01T15:00:00Z','endsAt':'2020-01-01T18:00:00Z'},400)]:
        call(feature,'POST','/api/showtimes',code,{**basebody,**change},headers={'X-User-Id':'cinemasaar-api-test-admin'},critical=False)
    call('Reject missing creator header','POST','/api/showtimes',400,basebody,critical=False)
    ids=[s['id'] for s in FIXTURES['showtimes']]
    script='pm.test("Both persisted screenings present", () => { const a=pm.response.json(); pm.expect(a.filter(x=>'+json.dumps(ids)+'.includes(x.id)).length).to.eql(2); });'
    for feature,path in [('Retrieve all showtimes','/api/showtimes'),('Verify screen showtimes','/api/showtimes/screen/'+sid),
                         ('Verify theater showtimes','/api/showtimes/theater/'+tid),('Verify upcoming showtimes','/api/showtimes/upcoming')]:
        call(feature,'GET',path,check=script)
    call('Verify screens persisted under theater','GET','/api/screens/theater/'+tid,check=expect_member(sid,{'theaterId':tid}))
    call('Verify theater persisted in list','GET','/api/theaters',check=expect_member(tid))
    call('Verify validation did not add seats','GET','/api/seats/screen/'+sid,
         check='pm.test("Seat count unchanged", () => pm.expect(pm.response.json()).to.have.length(30));')
    call('Reject layout changes after scheduling','POST','/api/seats',409,
         {'screenId':sid,'rowLabel':'D','seatNumber':1},critical=False)
    save()
    if any(r['result']=='FAIL' for r in ROWS):sys.exit(1)

if __name__=='__main__':
    try:main()
    except Exception as e:
        save();print('Stopped:',str(e),flush=True);sys.exit(1)
