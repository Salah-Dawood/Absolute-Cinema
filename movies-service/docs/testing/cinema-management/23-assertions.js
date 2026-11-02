pm.test("HTTP 200", () => pm.response.to.have.status(200));
pm.test("Contains expected persisted resource", () => { const r = pm.response.json().find(x => x.id === "6ae8798aa6456bb77994132d"); pm.expect(r).to.exist; pm.expect(r["movieId"]).to.eql("6ae8769ca6456bb77994130b");pm.expect(r["screenId"]).to.eql("6ae87901a6456bb77994130d");pm.expect(r["status"]).to.eql("SCHEDULED");});
