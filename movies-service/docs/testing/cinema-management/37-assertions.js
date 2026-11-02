pm.test("HTTP 200", () => pm.response.to.have.status(200));
pm.test("Seat count unchanged", () => pm.expect(pm.response.json()).to.have.length(30));
