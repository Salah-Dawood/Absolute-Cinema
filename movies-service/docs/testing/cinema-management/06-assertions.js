pm.test("HTTP 200", () => pm.response.to.have.status(200));
pm.test("Expected resource ID", () => pm.expect(pm.response.json().id).to.eql("6ae87901a6456bb77994130d"));pm.test("Screen belongs to theater", () => pm.expect(pm.response.json().theaterId).to.eql("6ae8789aa6456bb77994130c"));
