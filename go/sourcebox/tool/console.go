package tool

import (
	"fmt"
	"sourcebox/utils"
)

const ConsolePrintKey = "ConsolePrintKey"

type ConsoleContext struct {
	Context
}

// Write if key is ConsolePrintKey or "", will print val, otherwise will write to file, key will be filename
func (*ConsoleContext) Write(key string, val any) error {
	if key == ConsolePrintKey || key == "" {
		fmt.Printf("%s: %v", key, val)
	} else {
		f, err := utils.CreateFile(key)
		defer utils.CloseFile(f)
		if err != nil {
			return err
		}
		switch t := val.(type) {
		case string:
			_, e := f.WriteString(t)
			err = e
		case ContextWriteAction:
			err = t.Action(f)
		case []byte:
			_, e := f.Write(t)
			err = e
		default:
			err = fmt.Errorf("can not write %v", val)
		}

		return err
	}
	return nil
}
