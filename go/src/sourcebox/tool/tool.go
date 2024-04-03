package tool

import (
	"fmt"
	"io"
)

type Runner int

const (
	CLI Runner = iota
	WEB
	GUI
)

type Tool interface {
	Run() error
	Config() *Config
	Init()
}

type Context interface {
	// Write val kind: string, ContextWriteAction
	Write(key string, val any) error
}

type ContextWriteAction struct {
	Action func(writer io.Writer) error
}

type Config struct {
	Args    map[string]*Arg
	Context Context
	Runner  Runner
	Name    string
	Desc    string
}

func (c *Config) ReadArg(key string) (Arg, error) {
	a, ok := c.Args[key]
	if ok {
		return *a, nil
	}
	return Arg{}, fmt.Errorf("can not find %s arg", key)
}

func (c *Config) TestArg(key string, checkAction func(arg Arg) bool) bool {
	a, ok := c.Args[key]
	if ok {
		return checkAction(*a)
	}
	return false
}

// Arg argument
type Arg struct {
	Key      string
	Val      string
	Desc     string
	Required bool
	Demo     string
}
