package app

import (
	"os"
	"path/filepath"
)

func DataDir() (string, error) {
	base, err := os.UserHomeDir()
	if err != nil {
		return "", err
	}
	dir := filepath.Join(base, ".mytool", "desktop")
	if err := os.MkdirAll(dir, 0o700); err != nil {
		return "", err
	}
	return dir, nil
}
