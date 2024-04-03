package test

import (
	"fmt"
	"sourcebox/tool"
)

const BuildDir = "./../../../build/"

type Context struct {
	delegate    tool.ConsoleContext
	checkAction func(ctx Context) error
	Want        string
	Key         string
}

func NewContext(want string, action func(ctx Context) error) *Context {
	return &Context{
		delegate:    tool.ConsoleContext{},
		Want:        want,
		checkAction: action,
	}
}

func (c *Context) Write(key string, val any) error {
	c.Key = key

	err := c.delegate.Write(key, val)
	if err != nil {
		return err
	}

	if c.Want != "" {
		ret := fmt.Sprintf("%v", val)
		if c.Want != "" && ret != c.Want {
			return fmt.Errorf("value is %v but want %s", ret, c.Want)
		}
	} else if c.checkAction != nil {
		return c.checkAction(*c)
	}

	return nil
}

type Parameter struct {
	Name   string
	Config *tool.Config
}
