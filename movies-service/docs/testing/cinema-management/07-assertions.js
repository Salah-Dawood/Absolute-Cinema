pm.test("HTTP 200", () => pm.response.to.have.status(200));
pm.test("Contains expected persisted resource", () => { const r = pm.response.json().find(x => x.id === "6ae87901a6456bb77994130d"); pm.expect(r).to.exist; pm.expect(r["theaterId"]).to.eql("6ae8789aa6456bb77994130c");});
