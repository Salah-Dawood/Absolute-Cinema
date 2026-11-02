pm.test("HTTP 409", () => pm.response.to.have.status(409));
pm.test("Overlap message", () => pm.expect(pm.response.json().message).to.eql("Screen already has an overlapping showtime"));
