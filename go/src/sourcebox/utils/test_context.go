package utils

import (
	"fmt"
	"sourcebox/tool"
)

type TestContext struct {
	tool.Context
	Want string
}

func (c *TestContext) Write(key string, val any) error {
	ret := fmt.Sprintf("%v", val)
	if c.Want != "" && ret != c.Want {
		return fmt.Errorf("value is %v but want %s", ret, c.Want)
	}

	return nil
}
