package test

import (
	"fmt"
	"maps"
	"sourcebox/tool"
	"sourcebox/utils"
)

const BuildDir = "./../../../build/"

type Context struct {
	delegate tool.ConsoleContext
	// test class must call Write to invoke this function
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

func InitTool[T tool.Tool](t T, parameter Parameter) *T {
	t.Init()
	t.Config().Context = parameter.Config.Context
	maps.Copy(t.Config().Args, parameter.Config.Args)
	return &t
}

var CheckFileAction = func(ctx Context) error {
	if !utils.FileExists(ctx.Key) {
		return fmt.Errorf("file %v dose not exist", ctx.Key)
	}
	return nil
}

type Parameter struct {
	Name   string
	Config *tool.Config
}
