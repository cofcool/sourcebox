package converts

import (
	"errors"
	"flag"
	"sourcebox/tool"
)

type Converts struct {
	tool.Tool
}

type pipeline interface {
	run(args tool.Config) string
	name() string
}

var cmds = map[string]pipeline{
	"now": &now{},
}

func (*Converts) Run(args *tool.Config) error {
	f := flag.String("cmd", tool.FLAG_Default, "--cmd=now")
	flag.Parse()
	cmd := *f
	pipeline := cmds[cmd]
	if pipeline == nil {
		return errors.New("cmd error")
	}

	args.Context.Write(cmd, pipeline.run(*args))

	return nil
}

func (*Converts) Config() tool.Config {
	return tool.Config{
		Name: "converts",
		Config: []tool.Arg{
			{
				Key:      "cmd",
				Required: false,
			},
		},
	}
}
