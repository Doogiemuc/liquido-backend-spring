/**
 * create unique index constraints
 *
 * Usage:
 *   mongo liquido-test configureDB.js
 */

db.users.createIndex({ "email": 1 }, { unique: true })
db.areas.createIndex({ "title": 1 }, { unique: true })
db.delegations.createIndex({ "area":1, "fromUser":1, "toProxy":1 }, { unique: true })
db.ideas.createIndex({ "title":1 }, { unique: true })
db.laws.createIndex({ "title":1 }, { unique: true })
// db.ballots.createIndex({ "initialLawId":1, "voterHash":1 }, { unique: true })