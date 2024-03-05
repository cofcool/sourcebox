package main

import (
	"log"
	"sourcebox/converts"
	"sourcebox/tool"
)

func main() {
	c := converts.Converts{}
	cfg := tool.Config{
		Context: &tool.ConsoleContext{},
		Runner:  tool.CLI,
		Name:    "converts",
		Config:  make([]tool.Arg, 2),
	}

	// for i, v := range c.Config().Config {
	// 	fv := flag.String(v.Key, v.Val, v.Demo)
	// 	cfg.Config[i] = tool.Arg{
	// 		Key:      v.Key,
	// 		Val:      *fv,
	// 		Required: v.Required,
	// 	}
	// }

	e := c.Run(&cfg)
	if e != nil {
		log.Fatal(e)
	}
}
