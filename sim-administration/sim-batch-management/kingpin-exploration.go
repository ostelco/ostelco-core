//usr/bin/env go run "$0" "$@"; exit "$?"

package main

import "gopkg.in/alecthomas/kingpin.v2"

var (
	debug    = kingpin.Flag("debug", "enable debug mode").Default("false").Bool()
	serverIP = kingpin.Flag("server", "server address").Default("127.0.0.1").IP()

	register     = kingpin.Command("register", "Register a new user.")
	registerNick = register.Arg("nick", "nickname for user").Required().String()
	registerName = register.Arg("name", "name of user").Required().String()

	post        = kingpin.Command("post", "Post a message to a channel.")
	postImage   = post.Flag("image", "image to post").ExistingFile()
	postChannel = post.Arg("channel", "channel to post to").Required().String()
	postText    = post.Arg("text", "text to post").String()
)

func main() {
	switch kingpin.Parse() {
	// Register user
	case "register":
		println("register command")

	// Post message
	case "post":
		println("post command")
		/*
		if *postImage != nil {
		}
		if *postText != "" {
		}
		 */
	}
}
