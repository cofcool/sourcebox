package tool

import "fmt"

type ConsoleContext struct {
	Context
}

func (*ConsoleContext) Write(key string, val any) error {
	fmt.Printf("%s: %v", key, val)
	return nil
}
