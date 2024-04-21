package converts

import (
	"sourcebox/tool"
	"strings"
)

func runLower(args tool.Config) string {
	in, _ := args.ReadArg("in")
	return strings.ToLower(in.Val)
}
