pm.test("HTTP 200", () => pm.response.to.have.status(200));
pm.test("Both persisted screenings present", () => { const a=pm.response.json(); pm.expect(a.filter(x=>["6ae87987a6456bb77994132c", "6ae8798aa6456bb77994132d"].includes(x.id)).length).to.eql(2); });
