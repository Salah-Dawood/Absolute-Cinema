pm.test("HTTP 200", () => pm.response.to.have.status(200));
pm.test("Three rows of ten unique seats", () => { const s=pm.response.json(); pm.expect(s).to.have.length(30); pm.expect(new Set(s.map(x=>x.rowLabel+":"+x.seatNumber)).size).to.eql(30); pm.expect(s.every(x=>x.screenId==="6ae87901a6456bb77994130d")).to.eql(true); });
