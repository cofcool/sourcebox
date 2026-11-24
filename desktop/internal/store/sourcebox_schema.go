package store

const sourceboxSchema = `
CREATE TABLE IF NOT EXISTS sourcebox_notes (
  id TEXT PRIMARY KEY NOT NULL,
  content TEXT NOT NULL DEFAULT '',
  date TEXT,
  state TEXT NOT NULL DEFAULT 'NORMAL'
);
CREATE TABLE IF NOT EXISTS sourcebox_todos (
  id TEXT PRIMARY KEY NOT NULL,
  name TEXT NOT NULL DEFAULT '',
  state TEXT NOT NULL DEFAULT 'todo',
  remark TEXT,
  type TEXT NOT NULL DEFAULT 'todo',
  category TEXT,
  refs TEXT,
  start TEXT,
  end TEXT
);
CREATE TABLE IF NOT EXISTS sourcebox_commands (
  id TEXT PRIMARY KEY,
  cmd TEXT NOT NULL,
  alias TEXT,
  tags_json TEXT NOT NULL DEFAULT '[]',
  remark TEXT,
  frequency INTEGER,
  create_time TEXT,
  update_time TEXT
);
CREATE TABLE IF NOT EXISTS sourcebox_actions (
  id TEXT PRIMARY KEY NOT NULL,
  name TEXT NOT NULL,
  icon TEXT,
  action_index TEXT,
  device TEXT,
  type TEXT NOT NULL DEFAULT 'record',
  state TEXT NOT NULL DEFAULT 'todo',
  category TEXT,
  rating INTEGER,
  comments_json TEXT NOT NULL DEFAULT '[]',
  labels TEXT,
  refs TEXT,
  remark TEXT,
  start_at TEXT,
  end_at TEXT,
  duration INTEGER,
  create_time TEXT,
  update_time TEXT,
  other_json TEXT
);
CREATE TABLE IF NOT EXISTS sourcebox_action_comments (
  id TEXT PRIMARY KEY NOT NULL,
  action_id TEXT NOT NULL,
  content TEXT NOT NULL DEFAULT '',
  create_time TEXT
);
CREATE TABLE IF NOT EXISTS sourcebox_checklists (
  id TEXT PRIMARY KEY NOT NULL,
  action_id TEXT NOT NULL,
  content TEXT NOT NULL DEFAULT '',
  first_value INTEGER NOT NULL DEFAULT 0,
  last_value INTEGER NOT NULL DEFAULT 0,
  current_value INTEGER NOT NULL DEFAULT 0,
  create_time TEXT,
  update_time TEXT
);
CREATE TABLE IF NOT EXISTS sourcebox_habits (
  id TEXT,
  category TEXT NOT NULL DEFAULT 'check',
  action_id TEXT,
  note TEXT,
  unit TEXT,
  freq_num INTEGER,
  freq_den INTEGER,
  question TEXT,
  target_value INTEGER,
  target_type TEXT,
  create_time TEXT,
  update_time TEXT,
  PRIMARY KEY(id, action_id)
);
CREATE TABLE IF NOT EXISTS sourcebox_action_types (
  id TEXT PRIMARY KEY NOT NULL,
  name TEXT NOT NULL DEFAULT '',
  type TEXT NOT NULL DEFAULT '',
  create_time TEXT
);
CREATE TABLE IF NOT EXISTS sourcebox_recommendations (
  id TEXT PRIMARY KEY NOT NULL,
  data_id TEXT NOT NULL,
  table_name TEXT NOT NULL,
  create_time TEXT,
  update_time TEXT
);
CREATE TABLE IF NOT EXISTS sourcebox_photo_albums (
  id TEXT PRIMARY KEY NOT NULL,
  name TEXT NOT NULL DEFAULT '',
  type TEXT NOT NULL DEFAULT 'MANUAL',
  source_roots TEXT NOT NULL DEFAULT '',
  recursive INTEGER NOT NULL DEFAULT 1,
  cover_photo_id TEXT,
  status TEXT NOT NULL DEFAULT 'NORMAL',
  create_time TEXT,
  update_time TEXT
);
CREATE TABLE IF NOT EXISTS sourcebox_photos (
  id TEXT PRIMARY KEY NOT NULL,
  album_id TEXT NOT NULL DEFAULT '',
  file_name TEXT NOT NULL DEFAULT '',
  relative_path TEXT NOT NULL DEFAULT '',
  source_path TEXT NOT NULL DEFAULT '',
  mime_type TEXT,
  sha256 TEXT NOT NULL DEFAULT '',
  size INTEGER NOT NULL DEFAULT 0,
  width INTEGER,
  height INTEGER,
  taken_at TEXT,
  metadata_status TEXT NOT NULL DEFAULT 'SCANNED',
  metadata_json TEXT,
  status TEXT NOT NULL DEFAULT 'NORMAL',
  trashed_at TEXT,
  create_time TEXT,
  update_time TEXT
);
CREATE TABLE IF NOT EXISTS sourcebox_smart_albums (
  id TEXT PRIMARY KEY NOT NULL,
  name TEXT NOT NULL DEFAULT '',
  match_type TEXT NOT NULL DEFAULT 'ALL',
  rules_json TEXT NOT NULL DEFAULT '[]',
  enabled INTEGER NOT NULL DEFAULT 1,
  create_time TEXT,
  update_time TEXT
);
CREATE TABLE IF NOT EXISTS sourcebox_photo_backup_jobs (
  job_id TEXT PRIMARY KEY NOT NULL,
  status TEXT NOT NULL DEFAULT 'QUEUED',
  album_id TEXT NOT NULL DEFAULT '',
  source_directory TEXT,
  total INTEGER NOT NULL DEFAULT 0,
  completed INTEGER NOT NULL DEFAULT 0,
  failed INTEGER NOT NULL DEFAULT 0,
  error TEXT
);
CREATE TABLE IF NOT EXISTS sourcebox_photo_backup_config (
  id INTEGER PRIMARY KEY CHECK(id = 1),
  source_uri TEXT NOT NULL DEFAULT '',
  album_id TEXT NOT NULL DEFAULT '',
  wifi_only INTEGER NOT NULL DEFAULT 1,
  auto_enabled INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS sourcebox_tool_definitions (
  id TEXT PRIMARY KEY NOT NULL,
  description TEXT NOT NULL DEFAULT ''
);
CREATE TABLE IF NOT EXISTS sourcebox_tool_arguments (
  tool_id TEXT NOT NULL,
  key TEXT NOT NULL,
  description TEXT NOT NULL DEFAULT '',
  default_value TEXT NOT NULL DEFAULT '',
  required INTEGER NOT NULL DEFAULT 0,
  demo TEXT NOT NULL DEFAULT '',
  PRIMARY KEY(tool_id, key)
);
CREATE TABLE IF NOT EXISTS sourcebox_tool_events (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  source_json TEXT,
  tool TEXT NOT NULL DEFAULT '',
  action TEXT NOT NULL DEFAULT '',
  success INTEGER NOT NULL DEFAULT 1
);
CREATE TABLE IF NOT EXISTS sourcebox_habit_value_changes (
  checklist_id TEXT PRIMARY KEY NOT NULL,
  checklist_id_ref TEXT NOT NULL,
  content TEXT NOT NULL DEFAULT '',
  first_value INTEGER NOT NULL DEFAULT 0,
  last_value INTEGER NOT NULL DEFAULT 0,
  current_value INTEGER NOT NULL DEFAULT 0,
  create_time TEXT,
  update_time TEXT
);
CREATE TABLE IF NOT EXISTS sourcebox_metadata (
  key TEXT PRIMARY KEY NOT NULL,
  value TEXT NOT NULL DEFAULT ''
);
CREATE INDEX IF NOT EXISTS idx_sourcebox_actions_type_state_start
  ON sourcebox_actions(type, state, start_at);
CREATE INDEX IF NOT EXISTS idx_sourcebox_todos_state_start
  ON sourcebox_todos(state, start);
CREATE INDEX IF NOT EXISTS idx_sourcebox_comments_action
  ON sourcebox_action_comments(action_id, create_time);
CREATE INDEX IF NOT EXISTS idx_sourcebox_checklists_action
  ON sourcebox_checklists(action_id, first_value);
CREATE INDEX IF NOT EXISTS idx_sourcebox_habits_action
  ON sourcebox_habits(action_id);
CREATE INDEX IF NOT EXISTS idx_sourcebox_recommendations_data
  ON sourcebox_recommendations(table_name, data_id);
CREATE INDEX IF NOT EXISTS idx_sourcebox_photos_album_taken
  ON sourcebox_photos(album_id, taken_at);
`

func (s *Store) migrateSourcebox() error {
	_, err := s.db.Exec(sourceboxSchema)
	return err
}
