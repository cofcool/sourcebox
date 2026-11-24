package store

import (
	"path/filepath"
	"reflect"
	"testing"
)

func TestStoreCommandRoundTrip(t *testing.T) {
	s, err := Open(filepath.Join(t.TempDir(), "data"))
	if err != nil {
		t.Fatal(err)
	}
	defer s.Close()
	c := Command{ID: "test.custom", Name: "Echo", Kind: "custom", Executable: "echo", Args: []string{"hello"}, TimeoutSeconds: 5}
	if err := s.SaveCommand(c); err != nil {
		t.Fatal(err)
	}
	got, err := s.GetCommand(c.ID)
	if err != nil {
		t.Fatal(err)
	}
	if len(got.Args) != 1 || got.Args[0] != "hello" {
		t.Fatalf("unexpected args: %#v", got.Args)
	}
}

func TestSourceboxSnapshotMapsAndroidFieldsToSQLite(t *testing.T) {
	s, err := Open(filepath.Join(t.TempDir(), "data"))
	if err != nil {
		t.Fatal(err)
	}
	defer s.Close()

	date, category, target := "2026-11-24", "health", 7
	snapshot := SourceboxSnapshot{
		Actions: []SourceboxExport{{
			Record: SourceboxAction{
				ID: "action-1", Name: "Walk", Type: "habit", State: "doing",
				Category: &category, Comments: []string{"daily"}, Rating: &target,
				Start: &date, Duration: &target,
			},
			Comments: []SourceboxActionComment{{ID: "comment-1", ActionID: "action-1", Content: "good"}},
			Checklists: []SourceboxChecklist{{ID: "check-1", ActionID: "action-1", Content: "steps",
				First: 1, Last: 7, Current: 3}},
			Habit: &SourceboxHabit{ID: &date, ActionID: stringPointer("action-1"),
				Category: "check", FreqNum: &target, FreqDen: &target, TargetValue: &target},
		}},
		Notes:           []SourceboxNote{{ID: "note-1", Content: "memo", Date: &date, State: "NORMAL"}},
		ActionTypes:     []SourceboxActionType{{ID: "type-1", Name: "Habit", Type: "habit"}},
		Recommendations: []SourceboxRecommendation{{ID: "recommend-1", DataID: "action-1", Table: "action"}},
		Photos: SourceboxPhotoExport{
			Albums: []SourceboxPhotoAlbum{{ID: "album-1", Name: "Trips", Recursive: true}},
			Photos: []SourceboxPhoto{{ID: "photo-1", AlbumID: "album-1", FileName: "photo.jpg",
				RelativePath: "2026/photo.jpg", SourcePath: "/photos/photo.jpg", SHA256: "abc", Size: 42}},
			SmartAlbums: []SourceboxSmartAlbum{{ID: "smart-1", Name: "Recent", Enabled: true}},
		},
	}
	if err := s.SaveSourceboxSnapshot(snapshot); err != nil {
		t.Fatal(err)
	}
	counts, err := s.SourceboxCounts()
	if err != nil {
		t.Fatal(err)
	}
	if counts.Actions != 1 || counts.Habits != 1 || counts.Comments != 1 ||
		counts.Checklists != 1 || counts.Notes != 1 || counts.ActionTypes != 1 ||
		counts.Recommendations != 1 || counts.PhotoAlbums != 1 || counts.Photos != 1 ||
		counts.SmartAlbums != 1 {
		t.Fatalf("unexpected persisted counts: %+v", counts)
	}

	var name, actionType, comments string
	var duration int
	if err := s.db.QueryRow(`SELECT name,type,comments_json,duration FROM sourcebox_actions WHERE id=?`,
		"action-1").Scan(&name, &actionType, &comments, &duration); err != nil {
		t.Fatal(err)
	}
	if name != "Walk" || actionType != "habit" || comments != `["daily"]` || duration != 7 {
		t.Fatalf("action fields were not mapped: %q %q %q %d", name, actionType, comments, duration)
	}
	if err := s.db.QueryRow(`SELECT source_path,size FROM sourcebox_photos WHERE id=?`,
		"photo-1").Scan(&name, &duration); err != nil {
		t.Fatal(err)
	}
	if name != "/photos/photo.jpg" || duration != 42 {
		t.Fatalf("photo fields were not mapped: %q %d", name, duration)
	}
}

func TestSourceboxSchemaIncludesAndroidModelFields(t *testing.T) {
	s, err := Open(filepath.Join(t.TempDir(), "data"))
	if err != nil {
		t.Fatal(err)
	}
	defer s.Close()

	expected := map[string][]string{
		"sourcebox_notes":    {"id", "content", "date", "state"},
		"sourcebox_todos":    {"id", "name", "state", "remark", "type", "category", "refs", "start", "end"},
		"sourcebox_commands": {"id", "cmd", "alias", "tags_json", "remark", "frequency", "create_time", "update_time"},
		"sourcebox_actions": {"id", "name", "icon", "action_index", "device", "type", "state", "category",
			"rating", "comments_json", "labels", "refs", "remark", "start_at", "end_at", "duration",
			"create_time", "update_time", "other_json"},
		"sourcebox_action_comments": {"id", "action_id", "content", "create_time"},
		"sourcebox_checklists": {"id", "action_id", "content", "first_value", "last_value", "current_value",
			"create_time", "update_time"},
		"sourcebox_habits": {"id", "category", "action_id", "note", "unit", "freq_num", "freq_den",
			"question", "target_value", "target_type", "create_time", "update_time"},
		"sourcebox_action_types":    {"id", "name", "type", "create_time"},
		"sourcebox_recommendations": {"id", "data_id", "table_name", "create_time", "update_time"},
		"sourcebox_photo_albums": {"id", "name", "type", "source_roots", "recursive", "cover_photo_id",
			"status", "create_time", "update_time"},
		"sourcebox_photos": {"id", "album_id", "file_name", "relative_path", "source_path", "mime_type",
			"sha256", "size", "width", "height", "taken_at", "metadata_status", "metadata_json", "status",
			"trashed_at", "create_time", "update_time"},
		"sourcebox_smart_albums": {"id", "name", "match_type", "rules_json", "enabled", "create_time", "update_time"},
		"sourcebox_photo_backup_jobs": {"job_id", "status", "album_id", "source_directory", "total",
			"completed", "failed", "error"},
		"sourcebox_photo_backup_config": {"id", "source_uri", "album_id", "wifi_only", "auto_enabled"},
		"sourcebox_tool_definitions":    {"id", "description"},
		"sourcebox_tool_arguments":      {"tool_id", "key", "description", "default_value", "required", "demo"},
		"sourcebox_tool_events":         {"id", "source_json", "tool", "action", "success"},
		"sourcebox_habit_value_changes": {"checklist_id", "checklist_id_ref", "content", "first_value",
			"last_value", "current_value", "create_time", "update_time"},
	}
	for table, want := range expected {
		rows, err := s.db.Query("PRAGMA table_info(" + table + ")")
		if err != nil {
			t.Fatal(err)
		}
		var got []string
		for rows.Next() {
			var cid, notNull, primaryKey int
			var name, kind string
			var defaultValue any
			if err := rows.Scan(&cid, &name, &kind, &notNull, &defaultValue, &primaryKey); err != nil {
				rows.Close()
				t.Fatal(err)
			}
			got = append(got, name)
		}
		if err := rows.Close(); err != nil {
			t.Fatal(err)
		}
		if !reflect.DeepEqual(got, want) {
			t.Errorf("%s columns = %v; want %v", table, got, want)
		}
	}
}

func TestSourceboxSnapshotWithoutNotesPreservesLocalNotes(t *testing.T) {
	s, err := Open(filepath.Join(t.TempDir(), "data"))
	if err != nil {
		t.Fatal(err)
	}
	defer s.Close()
	if err := s.SaveSourceboxSnapshot(SourceboxSnapshot{
		Notes: []SourceboxNote{{ID: "local-note", Content: "keep me", State: "NORMAL"}},
	}); err != nil {
		t.Fatal(err)
	}
	if err := s.SaveSourceboxSnapshot(SourceboxSnapshot{}); err != nil {
		t.Fatal(err)
	}
	notes, err := s.ListSourceboxNotes(10)
	if err != nil {
		t.Fatal(err)
	}
	if len(notes) != 1 || notes[0].ID != "local-note" {
		t.Fatalf("unsynced notes were removed: %+v", notes)
	}
}

func stringPointer(value string) *string { return &value }
