package tool

import (
	"fmt"
)

type Runner int

const (
	WEB Runner = iota
	CLI
	GUI
)

type Tool interface {
	Run() error
	Config() *Config
}

type Context interface {
	Write(key string, val any) error
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

// Arg argument
type Arg struct {
	Key      string
	Val      string
	Desc     string
	Required bool
	Demo     string
}
