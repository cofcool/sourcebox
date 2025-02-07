package converts

import (
	"sourcebox/tool"
	"testing"
)

func Test_runUpper(t *testing.T) {
	args := tool.Config{
		Args: map[string]*tool.Arg{
			"in": {Key: "in", Val: "hELlO"},
		},
		Context: &tool.ConsoleContext{},
	}
	want := "HELLO"
	if got := runUpper(args); got != want {
		t.Errorf("runUpper() = %v, want %v", got, want)
	}
}
