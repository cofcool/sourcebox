package converts

import (
	"fmt"
	"sourcebox/tool"
)

type Converts struct {
	config *tool.Config
}

type commander func(args tool.Config) string

var cmds = map[string]commander{
	"now":       runNow,
	"morsecode": runMorseCode,
	"lower":     runLower,
	"upper":     runUpper,
}

func (c *Converts) Run() error {
	cmd, e := c.config.ReadArg("cmd")
	if e != nil {
		return e
	}
	pipeline, ok := cmds[cmd.Val]
	if !ok {
		return fmt.Errorf("cmd %s error", cmd.Val)
	}

	return c.config.Context.Write("", pipeline(*c.config))
}

func (c *Converts) Init() {
	c.config = &tool.Config{
		Name: "converts",
		Desc: "some simple utilities about string, like base64 encode",
		Args: map[string]*tool.Arg{
			"cmd": {
				Key:      "cmd",
				Desc:     "cmd=now",
				Required: true,
			},
			"in": {
				Key:      "in",
				Desc:     "in=demo",
				Required: false,
			},
			// morse code
			"mtype": {
				Key:      "mtype",
				Desc:     "mtype=en/de",
				Required: false,
			},
		},
	}
}

func (c *Converts) Config() *tool.Config {
	return c.config
}
