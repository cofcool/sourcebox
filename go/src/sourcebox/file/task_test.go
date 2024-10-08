package file

import (
	"sourcebox/tool"
	"sourcebox/utils/test"
	"testing"
)

func TestTask_Run(t *testing.T) {
	tests := []test.Parameter{
		{
			Name: "run",
			Config: &tool.Config{
				Context: test.NewContext("", nil),
				Args: map[string]*tool.Arg{
					"cmd": {
						Key: "cmd",
						Val: "echo $level-$count-$HOME-$timestamp-$idx-$timestamp_milli",
					},
					"count": {
						Key: "count",
						Val: "10",
					},
				},
			},
		},
		{
			Name: "runWithPerMs",
			Config: &tool.Config{
				Context: test.NewContext("", nil),
				Args: map[string]*tool.Arg{
					"cmd": {
						Key: "cmd",
						Val: "echo $level-$count-$HOME-$timestamp-$idx-$timestamp_milli",
					},
					"count": {
						Key: "count",
						Val: "10",
					},
					"perMs": {
						Key: "perMs",
						Val: "100",
					},
				},
			},
		},
		{
			Name: "runWithAfter",
			Config: &tool.Config{
				Context: test.NewContext("", nil),
				Args: map[string]*tool.Arg{
					"cmd": {
						Key: "cmd",
						Val: "echo $level-$count-$HOME-$timestamp-$idx-$timestamp_milli",
					},
					"count": {
						Key: "count",
						Val: "10",
					},
					"after": {
						Key: "after",
						Val: "echo after-$level",
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.Name, func(t *testing.T) {
			m := test.InitTool(&Task{}, tt)
			if err := (*m).Run(); err != nil {
				t.Errorf("Run() error: %v", err)
			}
		})
	}
}
