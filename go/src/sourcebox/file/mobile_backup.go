package file

import (
	"os/exec"
	"path"
	"sourcebox/tool"
	"strings"
)

// MobileBackup backup android phone files
// adb shell content query --uri content://sms/
// adb push/pull
type MobileBackup struct {
	config *tool.Config
}

const all = "all"

var types = map[string]string{
	"sms": "content://sms/",
}

func (m *MobileBackup) Run() error {
	outArg, _ := m.Config().ReadArg("out")
	typeArg, _ := m.Config().ReadArg("type")

	var typeStr []string
	if typeArg.Val == all {
		for k := range types {
			typeStr = append(typeStr, k)
		}
	} else {
		typeStr = strings.Split(typeArg.Val, ",")
	}

	outPath := outArg.Val
	outDir := path.Dir(outPath)
	for _, typeVal := range typeStr {
		val := types[typeVal]
		cmd := exec.Command("adb", "shell", "content", "query", "--uri", val)
		ret, e := cmd.Output()
		if e != nil {
			return e
		}

		e = m.config.Context.Write(path.Join(outDir, typeVal+".csv"), string(ret))
		if e != nil {
			return e
		}
	}

	return nil
}

func (m *MobileBackup) Config() *tool.Config {
	return m.config
}

func (m *MobileBackup) Init() {
	m.config = &tool.Config{
		Name: "mobileBackup",
		Desc: "backup android phone files by adb",
		Args: map[string]*tool.Arg{
			"type": {
				Key:  "type",
				Val:  "all",
				Desc: "backup type, like: sms, default is all",
			},
			"out": {
				Key:  "out",
				Val:  "./",
				Desc: "backup file path",
			},
		},
	}
}
