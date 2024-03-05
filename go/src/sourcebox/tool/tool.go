package tool

import "errors"

type Runner int

const (
	WEB Runner = iota
	CLI
	GUI
)

const FLAG_Default = "fd_nil"

type Tool interface {
	Run(args *Config) error
	Config() Config
}

type ToolContext interface {
	Write(key string, val interface{})
}

type Config struct {
	Config  []Arg
	Context ToolContext
	Runner  Runner
	Name    string
}

func (c *Config) ReadArg(key string) (Arg, error) {
	for _, v := range c.Config {
		if v.Key == key {
			return v, nil
		}
	}
	return Arg{}, errors.New("do not find")
}

type Arg struct {
	Key      string
	Val      string
	Desc     string
	Required bool
	Demo     string
}
