package main

//  A simple "finger exercise" to get to know sqlite in golang.  Purely
//  exploratory, no specific goal in sight.
//  NOTE: It will asap be rewritten to manage a persistent store of
//  batch parameters, and also be able to facilitate a workflow
//  sheparding the orders from inception to deployment.
//  Todo:
//   * Introduce common model package for existing structs.
//   * Extend model with metedata for persistence.
//   * Write DAO based on this file, but make it properly
//     unit tested using in-memory database.
//   * Figure out how to keep config somewhere safe without
//     having to type too much.

import (
	"database/sql"
	"fmt"
	"github.com/jmoiron/sqlx"
	"strconv"

	_ "github.com/mattn/go-sqlite3"
)

//  Sqlx https://jmoiron.github.io/sqlx/
// Person represents a person.
type Person struct {
	ID        int    `db:"id" json:"id"`
	Firstname string `db:"firstname" json:"firstname"`
	Lastname  string `db:"lastname" json:"lastname"`
}

func main_not() {

	// Get a reference to the database, create a table if it
	// doesn't exist already.

	var db *sqlx.DB

	// exactly the same as the built-in
	db, err := sqlx.Open("sqlite3", "./nraboy.db")

	database, err := sql.Open("sqlite3", "./nraboy.db")
	if err != nil {
		fmt.Errorf("open sql", err)
	}

	//
	statement, _ := database.Prepare("CREATE TABLE IF NOT EXISTS people (id INTEGER PRIMARY KEY, firstname TEXT, lastname TEXT)")
	_, err = statement.Exec()
	if err != nil {
		fmt.Errorf("Failed to create table: ", err)
	}

	// Insert a row.
	statement, _ = database.Prepare("INSERT INTO people (firstname, lastname) VALUES (?, ?)")
	_, err = statement.Exec("Nic", "Raboy")
	if err != nil {
		fmt.Errorf("Failed to  insert row: ", err)
	}

	// Query all the rows.
	rows, _ := database.Query("SELECT id, firstname, lastname FROM people")
	var id int
	var firstname string
	var lastname string
	for rows.Next() {
		rows.Scan(&id, &firstname, &lastname)
		if err != nil {
			fmt.Errorf("Failed to  scan row: ", err)
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
