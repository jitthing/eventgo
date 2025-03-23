// /lib/test-api.js using CommonJS
const { getEvent, getAllEvents } = require("./api");

(async () => {
	try {
		// Replace '1' with a valid event ID that exists in your API.
		const event = await getEvent(1);
		console.log("Event:", event);

		const events = await getAllEvents();
		console.log("All Events:", events);
	} catch (error) {
		console.error("Error fetching data:", error);
	}
})();
