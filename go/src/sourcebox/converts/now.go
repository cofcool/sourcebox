package converts

import (
	"sourcebox/tool"
	"strconv"
	"time"
)

type now struct {
}

func (n *now) run(args tool.Config) string {
	return strconv.FormatInt(time.Now().Unix(), 10)
}

func (n *now) name() string {
	return "now"
}
