package utils

import (
	"os"
	"path"
)

// CreateFile create new file, will check all dirs
func CreateFile(filepath string) (*os.File, error) {
	err := os.MkdirAll(path.Dir(filepath), 0775)
	if err != nil {
		return nil, err
	}
	return os.Create(filepath)
}

func CloseFile(f *os.File) {
	err := f.Close()
	if err != nil {
		panic(err)
	}
}

func FileExists(filename string) bool {
	_, err := os.Stat(filename)
	if err == nil {
		return true
	}
	if os.IsNotExist(err) {
		return false
	}
	return false
}
