pm.test("HTTP 200", () => pm.response.to.have.status(200));
pm.test("Contains expected persisted resource", () => { const r = pm.response.json().find(x => x.id === "6ae87987a6456bb77994132c"); pm.expect(r).to.exist; pm.expect(r["movieId"]).to.eql("6ae871d6a6456bb77994130a");pm.expect(r["screenId"]).to.eql("6ae87901a6456bb77994130d");pm.expect(r["status"]).to.eql("SCHEDULED");});
