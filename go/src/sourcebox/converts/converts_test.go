package converts

import (
	"maps"
	"sourcebox/tool"
	"sourcebox/utils/test"
	"testing"
)

func TestConverts_Run(t *testing.T) {
	tests := []test.Parameter{
		{
			Name: "now",
			Config: &tool.Config{
				Context: &test.Context{},
				Args: map[string]*tool.Arg{
					"cmd": {
						Key: "cmd",
						Val: "now",
					},
					"in": {
						Key: "in",
					},
				},
			},
		},
		{
			Name: "lower",
			Config: &tool.Config{
				Context: &test.Context{Want: "hello"},
				Args: map[string]*tool.Arg{
					"cmd": {
						Key: "cmd",
						Val: "lower",
					},
					"in": {
						Key: "in",
						Val: "hEllO",
					},
				},
			},
		},
		{
			Name: "upper",
			Config: &tool.Config{
				Context: &test.Context{Want: "HELLO"},
				Args: map[string]*tool.Arg{
					"cmd": {
						Key: "cmd",
						Val: "upper",
					},
					"in": {
						Key: "in",
						Val: "hEllO",
					},
				},
			},
		},
		{
			Name: "morsecode_en",
			Config: &tool.Config{
				Context: &test.Context{Want: "..  .-.. --- ...- .  -.-- --- ..-"},
				Args: map[string]*tool.Arg{
					"cmd": {
						Key: "cmd",
						Val: "morsecode",
					},
					"in": {
						Key: "in",
						Val: "i love you",
					},
					"mtype": {
						Key: "mtype",
						Val: "en",
					},
				},
			},
		},
		{
			Name: "morsecode_de",
			Config: &tool.Config{
				Context: &test.Context{Want: "I LOVE YOU"},
				Args: map[string]*tool.Arg{
					"cmd": {
						Key: "cmd",
						Val: "morsecode",
					},
					"in": {
						Key: "in",
						Val: "..  .-.. --- ...- .  -.-- --- ..-",
					},
					"mtype": {
						Key: "mtype",
						Val: "de",
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.Name, func(t *testing.T) {
			c := &Converts{}
			c.Init()
			c.config.Context = tt.Config.Context
			maps.Copy(c.config.Args, tt.Config.Args)
			if err := c.Run(); err != nil {
				t.Errorf("Run() error: %v", err)
			}
		})
	}
}
