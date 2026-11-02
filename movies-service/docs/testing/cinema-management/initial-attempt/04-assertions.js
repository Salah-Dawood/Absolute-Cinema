pm.test("HTTP 201", () => pm.response.to.have.status(201));
pm.test("Theater has an ID", () => pm.expect(pm.response.json().id).to.be.a("string"));
