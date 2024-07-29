package main

import (
	"github.com/urfave/cli/v2"
	"log"
	"os"
	"sourcebox/converts"
	"sourcebox/file"
	"sourcebox/git"
	"sourcebox/tool"
	"strings"
)

var (
	Version = ""
)

var tools = map[string]tool.Tool{
	"converts":       &converts.Converts{},
	"gitCommits2Log": &git.CommitLog{},
	"mobileBackup":   &file.MobileBackup{},
	"task":           &file.Task{},
}

var globalCfg = make(map[string]string)

func GetGlobalCfgVal(toolName string, key string) (string, bool) {
	v, ok := globalCfg[toolName+"."+key]
	return v, ok
}

var GlobalCfgPath = func() string {
	d, e := os.UserHomeDir()
	if e != nil {
		return ""
	}
	path := d + "/.mytool/mytool.cfg"
	err := readGlobalCfg(path)
	if err != nil {
		log.Printf("read config file %s error: %v", path, err)
	}
	return path
}()

func readGlobalCfg(path string) error {
	cfg, err := os.ReadFile(path)
	if err != nil {
		return err
	}

	for _, line := range strings.Split(string(cfg), "\n") {
		if line == "" {
			continue
		}
		kv := strings.Split(line, "=")
		globalCfg[kv[0]] = kv[1]
	}

	return nil
}

func main() {
	app := cli.NewApp()
	app.Name = "TheSourceBox"
	app.Authors = []*cli.Author{
		&cli.Author{Name: "CofCool"},
	}
	app.Version = Version
	app.Usage = "Some CLI utils"
	app.Flags = []cli.Flag{
		&cli.StringFlag{
			Name:        "config",
			Aliases:     []string{"f"},
			DefaultText: GlobalCfgPath,
			Usage:       "config file",
			Action: func(c *cli.Context, s string) error {
				GlobalCfgPath = s
				return readGlobalCfg(s)
			},
		},
	}

	var commands []*cli.Command
	for k, t := range tools {
		t.Init()
		config := t.Config()
		config.Runner = tool.CLI
		config.Name = k
		config.Context = &tool.ConsoleContext{}

		var flags []cli.Flag
		for s, arg := range config.Args {
			gCfg, _ := GetGlobalCfgVal(k, arg.Key)
			arg.Val = gCfg
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

	app.Commands = commands

	err := app.Run(os.Args)
	if err != nil {
		log.Fatal(err)
	}
}
