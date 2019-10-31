package storage

import (
	"database/sql"
	"fmt"
	"github.com/jmoiron/sqlx"
	"strconv"
)

//  Sqlx https://jmoiron.github.io/sqlx/
// Person represents a person.
type Person struct {
	ID        int    `db:"id" json:"id"`
	Firstname string `db:"firstname" json:"firstname"`
	Lastname  string `db:"lastname" json:"lastname"`
}

func doTheBatchThing() {

	fmt.Println("The batching getting started")

	// Get a reference to the database, create a table if it
	// doesn't exist already.

	var db *sqlx.DB

	// exactly the same as the built-in
	db, err := sqlx.Open("sqlite3", "./nraboy.db")

	database, err := sql.Open("sqlite3", "./nraboy.db")
	if err != nil {
		panic("COuldn't open sql")
	}

	//
	statement, _ := database.Prepare("CREATE TABLE IF NOT EXISTS people (id INTEGER PRIMARY KEY, firstname TEXT, lastname TEXT)")
	_, err = statement.Exec()
	if err != nil {
		panic("COuldn't create table")
	}

	// Insert a row.
	statement, _ = database.Prepare("INSERT INTO people (firstname, lastname) VALUES (?, ?)")
	_, err = statement.Exec("Nic", "Raboy")
	if err != nil {
		panic("Failed to  insert row: ")
	}

	// Query all the rows.
	rows, _ := database.Query("SELECT id, firstname, lastname FROM people")
	var id int
	var firstname string
	var lastname string
	for rows.Next() {
		rows.Scan(&id, &firstname, &lastname)
		if err != nil {
			panic("Failed to  scan row: ")
		}
		fmt.Println(strconv.Itoa(id) + ": " + firstname + " " + lastname)
	}

	fmt.Print("Foo->")
	rowz, err := db.Queryx("SELECT * FROM people")
	for rowz.Next() {
		var p Person
		err = rowz.StructScan(&p)
		fmt.Println("The p = ", p)
	}
}
