package main

import (
	"log"
	"os"
	"sourcebox/converts"
	"sourcebox/tool"

	"github.com/urfave/cli/v2"
)

var tools = map[string]tool.Tool{
	"converts": &converts.Converts{},
}

func main() {
	var commands []*cli.Command
	for k, t := range tools {
		config := t.Config()
		config.Runner = tool.CLI
		config.Name = k
		config.Context = &tool.ConsoleContext{}

		var flags []cli.Flag
		for s, arg := range config.Args {
			flags = append(flags, &cli.StringFlag{
				Name:     s,
				Usage:    arg.Demo,
				Value:    arg.Val,
				Required: arg.Required,
			})
		}

		cmd := cli.Command{
			Name:    k,
			Aliases: []string{},
			Usage:   config.Desc,
			Flags:   flags,
			Action: func(c *cli.Context) error {
				for s, arg := range config.Args {
					arg.Val = c.String(s)
				}

				return t.Run()
			},
		}
		commands = append(commands, &cmd)
	}

	app := cli.NewApp()
	app.Name = "The Source Box"
	app.Flags = []cli.Flag{
		&cli.StringFlag{
			Name:    "tool",
			Aliases: []string{"t"},
			Usage:   "tool name",
			Action: func(c *cli.Context, s string) error {
				return tools[s].Run()
			},
		},
	}

	app.Commands = commands

	err := app.Run(os.Args)
	if err != nil {
		log.Fatal(err)
	}
}
