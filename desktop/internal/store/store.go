package store

import (
	"database/sql"
	"encoding/json"
	"errors"
	"fmt"
	"path/filepath"
	"os"
	"time"

	_ "modernc.org/sqlite"
)

type Store struct {
	db *sql.DB
}

type Command struct {
	ID             string   `json:"id"`
	Name           string   `json:"name"`
	Kind           string   `json:"kind"` // builtin | custom
	Executable     string   `json:"executable"`
	Args           []string `json:"args"`
	WorkingDir     string   `json:"workingDir"`
	TimeoutSeconds int      `json:"timeoutSeconds"`
	CreatedAt      string   `json:"createdAt"`
}

type History struct {
	ID         int64  `json:"id"`
	CommandID  string `json:"commandId"`
	Command    string `json:"command"`
	ExitCode   int    `json:"exitCode"`
	Stdout     string `json:"stdout"`
	Stderr     string `json:"stderr"`
	DurationMS int64  `json:"durationMs"`
	CreatedAt  string `json:"createdAt"`
}

func Open(dataDir string) (*Store, error) {
	if err := os.MkdirAll(dataDir, 0o700); err != nil { return nil, err }
	dbPath := filepath.Join(dataDir, "app.db")
	db, err := sql.Open("sqlite", dbPath)
	if err != nil {
		return nil, err
	}
	db.SetMaxOpenConns(1)

	s := &Store{db: db}
	if _, err := db.Exec(`PRAGMA journal_mode=WAL; PRAGMA foreign_keys=ON;`); err != nil {
		db.Close()
		return nil, err
	}
	if err := s.migrate(); err != nil {
		db.Close()
		return nil, err
	}
	if err := s.seed(); err != nil {
		db.Close()
		return nil, err
	}
	return s, nil
}

func (s *Store) Close() error { return s.db.Close() }

func (s *Store) migrate() error {
	const schema = `
CREATE TABLE IF NOT EXISTS app_config (
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL,
  updated_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS commands (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  kind TEXT NOT NULL CHECK(kind IN ('builtin','custom')),
  executable TEXT NOT NULL DEFAULT '',
  args_json TEXT NOT NULL DEFAULT '[]',
  working_dir TEXT NOT NULL DEFAULT '',
  timeout_seconds INTEGER NOT NULL DEFAULT 15,
  created_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS command_history (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  command_id TEXT NOT NULL,
  command_name TEXT NOT NULL,
  exit_code INTEGER NOT NULL,
  stdout TEXT NOT NULL,
  stderr TEXT NOT NULL,
  duration_ms INTEGER NOT NULL,
  created_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_command_history_created
  ON command_history(created_at DESC);
`
	_, err := s.db.Exec(schema)
	return err
}

func (s *Store) seed() error {
	var count int
	if err := s.db.QueryRow(`SELECT COUNT(*) FROM commands WHERE kind='builtin'`).Scan(&count); err != nil {
		return err
	}
	if count > 0 {
		return nil
	}
	now := time.Now().UTC().Format(time.RFC3339Nano)
	builtins := []Command{
		{ID: "system.info", Name: "System information", Kind: "builtin", TimeoutSeconds: 5, CreatedAt: now},
		{ID: "system.cwd", Name: "Current directory", Kind: "builtin", TimeoutSeconds: 5, CreatedAt: now},
		{ID: "git.status", Name: "Git status (current directory)", Kind: "builtin", Executable: "git", Args: []string{"status", "--short"}, TimeoutSeconds: 15, CreatedAt: now},
	}
	for _, c := range builtins {
		if err := s.SaveCommand(c); err != nil {
			return err
		}
	}
	return nil
}

func (s *Store) SaveCommand(c Command) error {
	args, err := json.Marshal(c.Args)
	if err != nil {
		return err
	}
	if c.TimeoutSeconds <= 0 || c.TimeoutSeconds > 300 {
		return errors.New("timeoutSeconds must be between 1 and 300")
	}
	if c.CreatedAt == "" {
		c.CreatedAt = time.Now().UTC().Format(time.RFC3339Nano)
	}
	_, err = s.db.Exec(`
INSERT INTO commands(id,name,kind,executable,args_json,working_dir,timeout_seconds,created_at)
VALUES(?,?,?,?,?,?,?,?)
ON CONFLICT(id) DO UPDATE SET
 name=excluded.name, executable=excluded.executable, args_json=excluded.args_json,
 working_dir=excluded.working_dir, timeout_seconds=excluded.timeout_seconds
`, c.ID, c.Name, c.Kind, c.Executable, string(args), c.WorkingDir, c.TimeoutSeconds, c.CreatedAt)
	return err
}

func (s *Store) ListCommands() ([]Command, error) {
	rows, err := s.db.Query(`SELECT id,name,kind,executable,args_json,working_dir,timeout_seconds,created_at
FROM commands ORDER BY CASE kind WHEN 'builtin' THEN 0 ELSE 1 END, name`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := make([]Command, 0)
	for rows.Next() {
		var c Command
		var args string
		if err := rows.Scan(&c.ID, &c.Name, &c.Kind, &c.Executable, &args, &c.WorkingDir, &c.TimeoutSeconds, &c.CreatedAt); err != nil {
			return nil, err
		}
		if err := json.Unmarshal([]byte(args), &c.Args); err != nil {
			return nil, fmt.Errorf("decode args for %s: %w", c.ID, err)
		}
		out = append(out, c)
	}
	return out, rows.Err()
}

func (s *Store) GetCommand(id string) (Command, error) {
	var c Command
	var args string
	err := s.db.QueryRow(`SELECT id,name,kind,executable,args_json,working_dir,timeout_seconds,created_at
FROM commands WHERE id=?`, id).Scan(&c.ID, &c.Name, &c.Kind, &c.Executable, &args, &c.WorkingDir, &c.TimeoutSeconds, &c.CreatedAt)
	if err != nil {
		return c, err
	}
	err = json.Unmarshal([]byte(args), &c.Args)
	return c, err
}

func (s *Store) DeleteCustomCommand(id string) error {
	res, err := s.db.Exec(`DELETE FROM commands WHERE id=? AND kind='custom'`, id)
	if err != nil {
		return err
	}
	n, _ := res.RowsAffected()
	if n == 0 {
		return errors.New("custom command not found")
	}
	return nil
}

func (s *Store) AddHistory(h History) error {
	_, err := s.db.Exec(`INSERT INTO command_history
(command_id,command_name,exit_code,stdout,stderr,duration_ms,created_at)
VALUES(?,?,?,?,?,?,?)`, h.CommandID, h.Command, h.ExitCode, h.Stdout, h.Stderr, h.DurationMS, time.Now().UTC().Format(time.RFC3339Nano))
	return err
}

func (s *Store) ListHistory(limit int) ([]History, error) {
	if limit <= 0 || limit > 200 {
		limit = 50
	}
	rows, err := s.db.Query(`SELECT id,command_id,command_name,exit_code,stdout,stderr,duration_ms,created_at
FROM command_history ORDER BY id DESC LIMIT ?`, limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := make([]History, 0)
	for rows.Next() {
		var h History
		if err := rows.Scan(&h.ID, &h.CommandID, &h.Command, &h.ExitCode, &h.Stdout, &h.Stderr, &h.DurationMS, &h.CreatedAt); err != nil {
			return nil, err
		}
		out = append(out, h)
	}
	return out, rows.Err()
}

func (s *Store) SetConfig(key, value string) error {
	_, err := s.db.Exec(`INSERT INTO app_config(key,value,updated_at) VALUES(?,?,?)
ON CONFLICT(key) DO UPDATE SET value=excluded.value, updated_at=excluded.updated_at`,
		key, value, time.Now().UTC().Format(time.RFC3339Nano))
	return err
}

func (s *Store) GetConfig(key string) (string, error) {
	var value string
	err := s.db.QueryRow(`SELECT value FROM app_config WHERE key=?`, key).Scan(&value)
	return value, err
}
