package service

import (
	"bytes"
	"crypto/rand"
	"encoding/hex"
	"context"
	"errors"
	"encoding/json"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"time"

	"github.com/example/thesourcebox-desktop/internal/store"
)

type CommandResult struct {
	CommandID  string `json:"commandId"`
	Command    string `json:"command"`
	ExitCode   int    `json:"exitCode"`
	Stdout     string `json:"stdout"`
	Stderr     string `json:"stderr"`
	DurationMS int64  `json:"durationMs"`
}

type cappedBuffer struct {
	buf bytes.Buffer
	max int
}

func (b *cappedBuffer) Write(p []byte) (int, error) {
	original := len(p)
	remaining := b.max - b.buf.Len()
	if remaining > 0 {
		if len(p) > remaining {
			p = p[:remaining]
		}
		_, _ = b.buf.Write(p)
	}
	return original, nil
}

func (b *cappedBuffer) String() string {
	s := b.buf.String()
	if len(s) >= b.max {
		s += "\n[output truncated]"
	}
	return s
}

func (s *Services) CreateCustomCommand(name, executable, argsJSON, workingDir string, timeoutSeconds int) (store.Command, error) {
	name = strings.TrimSpace(name)
	executable = strings.TrimSpace(executable)
	if name == "" || executable == "" {
		return store.Command{}, errors.New("name and executable are required")
	}
	args, err := decodeArgs(argsJSON)
	if err != nil {
		return store.Command{}, err
	}
	if timeoutSeconds <= 0 || timeoutSeconds > 300 {
		timeoutSeconds = 30
	}
	if workingDir != "" {
		abs, err := filepath.Abs(workingDir)
		if err != nil {
			return store.Command{}, err
		}
		workingDir = abs
	}
	c := store.Command{
		ID: "custom-" + newID(), Name: name, Kind: "custom",
		Executable: executable, Args: args, WorkingDir: workingDir,
		TimeoutSeconds: timeoutSeconds,
	}
	if err := s.store.SaveCommand(c); err != nil {
		return store.Command{}, err
	}
	return c, nil
}

func (s *Services) RunCommand(id string) (CommandResult, error) {
	c, err := s.store.GetCommand(id)
	if err != nil {
		return CommandResult{}, err
	}
	start := time.Now()
	result := CommandResult{CommandID: c.ID, Command: c.Name, ExitCode: -1}

	if c.Kind == "builtin" {
		switch c.ID {
		case "system.info":
			result.ExitCode = 0
			result.Stdout = fmt.Sprintf("OS: %s\nArchitecture: %s\nGo: %s\n", runtime.GOOS, runtime.GOARCH, runtime.Version())
		case "system.cwd":
			dir, err := os.Getwd()
			if err != nil {
				result.Stderr = err.Error()
			} else {
				result.ExitCode, result.Stdout = 0, dir
			}
		default:
			result = s.runProcess(c, result)
		}
	} else {
		result = s.runProcess(c, result)
	}

	result.DurationMS = time.Since(start).Milliseconds()
	_ = s.store.AddHistory(store.History{
		CommandID: result.CommandID, Command: result.Command, ExitCode: result.ExitCode,
		Stdout: result.Stdout, Stderr: result.Stderr, DurationMS: result.DurationMS,
	})
	return result, nil
}

func (s *Services) runProcess(c store.Command, result CommandResult) CommandResult {
	if c.Executable == "" {
		result.Stderr = "executable is empty"
		return result
	}
	executable, err := exec.LookPath(c.Executable)
	if err != nil {
		result.Stderr = err.Error()
		return result
	}
	timeout := time.Duration(c.TimeoutSeconds) * time.Second
	if timeout <= 0 || timeout > 300*time.Second {
		timeout = 30 * time.Second
	}
	ctx, cancel := context.WithTimeout(context.Background(), timeout)
	defer cancel()

	cmd := exec.CommandContext(ctx, executable, c.Args...)
	if c.WorkingDir != "" {
		cmd.Dir = c.WorkingDir
	}
	stdout, stderr := &cappedBuffer{max: 256 * 1024}, &cappedBuffer{max: 256 * 1024}
	cmd.Stdout, cmd.Stderr = stdout, stderr
	err = cmd.Run()
	result.Stdout, result.Stderr = stdout.String(), stderr.String()
	if err == nil {
		result.ExitCode = 0
		return result
	}
	var exitErr *exec.ExitError
	if errors.As(err, &exitErr) {
		result.ExitCode = exitErr.ExitCode()
	} else {
		result.ExitCode = -1
	}
	if ctx.Err() != nil {
		result.Stderr += "\n" + ctx.Err().Error()
	}
	if result.Stderr == "" {
		result.Stderr = err.Error()
	}
	return result
}

func decodeArgs(raw string) ([]string, error) {
	if strings.TrimSpace(raw) == "" {
		return []string{}, nil
	}
	var args []string
	if err := json.Unmarshal([]byte(raw), &args); err != nil {
		return nil, fmt.Errorf("args must be a JSON string array: %w", err)
	}
	if len(args) > 100 {
		return nil, errors.New("too many arguments (max 100)")
	}
	return args, nil
}

func newID() string {
	var b [16]byte
	if _, err := rand.Read(b[:]); err != nil {
		return fmt.Sprintf("%d", time.Now().UnixNano())
	}
	return hex.EncodeToString(b[:])
}
