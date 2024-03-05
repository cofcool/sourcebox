package converts

import (
	"sourcebox/tool"
	"strconv"
	"testing"
	"time"
)

func TestRun(t *testing.T) {
	n := now{}
	r := n.run(tool.Config{})
	if r != strconv.FormatInt(time.Now().Unix(), 10) {
		t.Error("now not equal now()")
	}
}
