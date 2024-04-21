package converts

import (
	"sourcebox/tool"
	"strings"
)

func runUpper(args tool.Config) string {
	in, _ := args.ReadArg("in")
	return strings.ToUpper(in.Val)
}
