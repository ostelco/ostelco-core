module.exports = {
	globals: {
		"ts-jest": {
			tsConfigFile: "tsconfig.json"
		}
	},
	transform: {
		"^.+\\.(ts|tsx)$": "ts-jest"
	},
	moduleFileExtensions: [
		"ts",
		"json",
		"js"
	],
	testMatch: [
	  "**/test/**/*.test.(ts|js)"
	],
	testEnvironment: 'node'
};

