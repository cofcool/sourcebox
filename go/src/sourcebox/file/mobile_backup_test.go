package file

import (
	"path/filepath"
	"sourcebox/tool"
	"sourcebox/utils/test"
	"testing"
)

func TestMobileBackup_Run(t *testing.T) {
	tests := []test.Parameter{
		{
			Name: "run",
			Config: &tool.Config{
				Context: test.NewContext("", test.CheckFileAction),
				Args: map[string]*tool.Arg{
					"out": {
						Key: "out",
						Val: filepath.Join(test.BuildDir, "sms.csv"),
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.Name, func(t *testing.T) {
			m := test.InitTool(&MobileBackup{}, tt)
			if err := (*m).Run(); err != nil {
				t.Errorf("Run() error: %v", err)
			}
		})
	}
}
