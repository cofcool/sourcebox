package converts

import (
	"sourcebox/tool"
	"strconv"
	"time"
)

func runNow(args tool.Config) string {
	return strconv.FormatInt(time.Now().Unix(), 10)
}
