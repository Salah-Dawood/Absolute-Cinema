pm.test("HTTP 200", () => pm.response.to.have.status(200));
pm.test("Expected resource ID", () => pm.expect(pm.response.json().id).to.eql("6ae8769ca6456bb77994130b"));pm.test("Movie is showing", () => pm.expect(pm.response.json().isShowing).to.eql(true));
