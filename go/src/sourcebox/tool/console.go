package tool

import "fmt"

type ConsoleContext struct {
	ToolContext
}

func (*ConsoleContext) Write(key string, val interface{}) {
	fmt.Printf("%s: %v", key, val)
}
