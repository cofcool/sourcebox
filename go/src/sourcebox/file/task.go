package file

import (
	"fmt"
	"mvdan.cc/sh/v3/shell"
	"os"
	"os/exec"
	"sourcebox/tool"
	"strconv"
	"time"
)

type Task struct {
	config *tool.Config
}

func (m *Task) Run() error {
	cmdIn, err := m.config.ReadArg("cmd")
	if err != nil {
		return err
	}

	levelIn, _ := m.config.ReadArg("level")
	countIn, _ := m.config.ReadArg("count")

	level, err := strconv.Atoi(levelIn.Val)
	count, err := strconv.Atoi(countIn.Val)
	if err != nil {
		return err
	}
	if level <= 0 {
		level = 1
	}
	fmt.Printf("task execute times: %d x %d\n", level, count)
	idx := 0
	for i := 0; i < level; i++ {
		for j := 0; j < count; j++ {
			s := cmdIn.Val
			idx++
			cmds, err := shell.Fields(s, func(s string) string {
				switch s {
				case "level":
					return fmt.Sprintf("%d", i)
				case "count":
					return fmt.Sprintf("%d", j)
				case "idx":
					return fmt.Sprintf("%d", idx)
				case "timestamp":
					return fmt.Sprintf("%d", time.Now().Unix())
				case "timestamp_milli":
					return fmt.Sprintf("%d", time.Now().UnixMilli())
				default:
					return os.Getenv(s)
				}
			})
			if err != nil {
				return err
			}
			fmt.Println(cmds)
			cmd := exec.Command(cmds[0], cmds[1:]...)
			o, err := cmd.Output()
			if err != nil {
				fmt.Println(err.Error())
				continue
			}
			fmt.Printf("result is: %s\n", o)
		}
	}

	return nil
}

func (m *Task) Config() *tool.Config {
	return m.config
}

func (m *Task) Init() {
	m.config = &tool.Config{
		Name: "task",
		Desc: "repeat execute task",
		Args: map[string]*tool.Arg{
			"cmd": {
				Key:      "cmd",
				Required: true,
				Desc:     "wait to execute command, like curl https://demo.com/$idx/$level, variables: count, level, idx, timestamp",
			},
			"level": {
				Key:  "level",
				Val:  "1",
				Desc: "loop level",
			},
			"count": {
				Key:      "count",
				Required: true,
				Desc:     "single loop count",
			},
		},
	}
}
