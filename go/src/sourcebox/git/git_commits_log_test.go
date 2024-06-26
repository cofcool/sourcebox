package git

import (
	"path/filepath"
	"sourcebox/tool"
	"sourcebox/utils/test"
	"testing"
)

func TestCommitLog_Run(t *testing.T) {
	tests := []test.Parameter{
		{
			Name: "run",
			Config: &tool.Config{
				Context: test.NewContext("", test.CheckFileAction),
				Args: map[string]*tool.Arg{
					"path": {
						Key: "path",
						Val: "./",
					},
					"out": {
						Key: "out",
						Val: filepath.Join(test.BuildDir, "changelog.md"),
					},
				},
			},
		},
		{
			Name: "runWithLogId",
			Config: &tool.Config{
				Context: test.NewContext("", test.CheckFileAction),
				Args: map[string]*tool.Arg{
					"path": {
						Key: "path",
						Val: "./",
					},
					"logId": {
						Key: "logId",
						Val: "#log",
					},
					"out": {
						Key: "out",
						Val: filepath.Join(test.BuildDir, "changelogWithLogId.md"),
					},
				},
			},
		},
		{
			Name: "runWithUser",
			Config: &tool.Config{
				Context: test.NewContext("", test.CheckFileAction),
				Args: map[string]*tool.Arg{
					"path": {
						Key: "path",
						Val: "./",
					},
					"user": {
						Key: "user",
						Val: "demo",
					},
					"out": {
						Key: "out",
						Val: filepath.Join(test.BuildDir, "changelogWithUser.md"),
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.Name, func(t *testing.T) {
			c := test.InitTool(&CommitLog{}, tt)
			if err := (*c).Run(); err != nil {
				t.Errorf("Run() error: %v", err)
			}
		})
	}
}
