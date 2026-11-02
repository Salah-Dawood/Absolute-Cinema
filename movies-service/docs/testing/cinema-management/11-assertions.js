pm.test("HTTP 409", () => pm.response.to.have.status(409));
pm.test("Duplicate detected", () => pm.expect(pm.response.json().message).to.eql("Seat already exists"));
