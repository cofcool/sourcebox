package store

import (
	"database/sql"
	"encoding/json"
	"fmt"
)

type SourceboxNote struct {
	ID      string  `json:"id"`
	Content string  `json:"content"`
	Date    *string `json:"date"`
	State   string  `json:"state"`
}

type SourceboxTodo struct {
	ID       string  `json:"id"`
	Name     string  `json:"name"`
	State    string  `json:"state"`
	Remark   *string `json:"remark"`
	Type     string  `json:"type"`
	Category *string `json:"category"`
	Refs     *string `json:"refs"`
	Start    *string `json:"start"`
	End      *string `json:"end"`
}

type SourceboxCommand struct {
	ID         *string  `json:"id"`
	Cmd        string   `json:"cmd"`
	Alias      *string  `json:"alias"`
	Tags       []string `json:"tags"`
	Remark     *string  `json:"remark"`
	Frequency  *int     `json:"frequency"`
	CreateTime *string  `json:"createTime"`
	UpdateTime *string  `json:"updateTime"`
}

type SourceboxAction struct {
	ID         string   `json:"id"`
	Name       string   `json:"name"`
	Icon       *string  `json:"icon"`
	Index      *string  `json:"index"`
	Device     *string  `json:"device"`
	Type       string   `json:"type"`
	State      string   `json:"state"`
	Category   *string  `json:"category"`
	Rating     *int     `json:"rating"`
	Comments   []string `json:"comments"`
	Labels     *string  `json:"labels"`
	Refs       *string  `json:"refs"`
	Remark     *string  `json:"remark"`
	Start      *string  `json:"start"`
	End        *string  `json:"end"`
	Duration   *int     `json:"duration"`
	CreateTime *string  `json:"createTime"`
	UpdateTime *string  `json:"updateTime"`
}

type SourceboxActionComment struct {
	ID         string  `json:"id"`
	ActionID   string  `json:"actionId"`
	Content    string  `json:"content"`
	CreateTime *string `json:"createTime"`
}

type SourceboxChecklist struct {
	ID         string  `json:"id"`
	ActionID   string  `json:"actionId"`
	Content    string  `json:"content"`
	First      int     `json:"first"`
	Last       int     `json:"last"`
	Current    int     `json:"current"`
	CreateTime *string `json:"createTime"`
	UpdateTime *string `json:"updateTime"`
}

type SourceboxHabit struct {
	ID          *string `json:"id"`
	ActionID    *string `json:"actionId"`
	Category    string  `json:"category"`
	Note        *string `json:"note"`
	Unit        *string `json:"unit"`
	FreqNum     *int    `json:"freqNum"`
	FreqDen     *int    `json:"freqDen"`
	Question    *string `json:"question"`
	TargetValue *int    `json:"targetValue"`
	TargetType  *string `json:"targetType"`
	CreateTime  *string `json:"createTime"`
	UpdateTime  *string `json:"updateTime"`
}

type SourceboxActionType struct {
	ID         string  `json:"id"`
	Name       string  `json:"name"`
	Type       string  `json:"type"`
	CreateTime *string `json:"createTime"`
}

type SourceboxRecommendation struct {
	ID         string  `json:"id"`
	DataID     string  `json:"dataId"`
	Table      string  `json:"table"`
	CreateTime *string `json:"createTime"`
	UpdateTime *string `json:"updateTime"`
}

type SourceboxPhotoAlbum struct {
	ID           string  `json:"id"`
	Name         string  `json:"name"`
	Type         string  `json:"type"`
	SourceRoots  string  `json:"sourceRoots"`
	Recursive    bool    `json:"recursive"`
	CoverPhotoID *string `json:"coverPhotoId"`
	Status       string  `json:"status"`
	CreateTime   *string `json:"createTime"`
	UpdateTime   *string `json:"updateTime"`
}

type SourceboxPhoto struct {
	ID             string  `json:"id"`
	AlbumID        string  `json:"albumId"`
	FileName       string  `json:"fileName"`
	RelativePath   string  `json:"relativePath"`
	SourcePath     string  `json:"sourcePath"`
	MimeType       *string `json:"mimeType"`
	SHA256         string  `json:"sha256"`
	Size           int64   `json:"size"`
	Width          *int    `json:"width"`
	Height         *int    `json:"height"`
	TakenAt        *string `json:"takenAt"`
	MetadataStatus string  `json:"metadataStatus"`
	MetadataJSON   *string `json:"metadataJson"`
	Status         string  `json:"status"`
	TrashedAt      *string `json:"trashedAt"`
	CreateTime     *string `json:"createTime"`
	UpdateTime     *string `json:"updateTime"`
}

type SourceboxSmartAlbum struct {
	ID         string  `json:"id"`
	Name       string  `json:"name"`
	MatchType  string  `json:"matchType"`
	RulesJSON  string  `json:"rulesJson"`
	Enabled    bool    `json:"enabled"`
	CreateTime *string `json:"createTime"`
	UpdateTime *string `json:"updateTime"`
}

type SourceboxBackupJob struct {
	JobID           string  `json:"jobId"`
	Status          string  `json:"status"`
	AlbumID         string  `json:"albumId"`
	SourceDirectory *string `json:"sourceDirectory"`
	Total           int     `json:"total"`
	Completed       int     `json:"completed"`
	Failed          int     `json:"failed"`
	Error           *string `json:"error"`
}

type SourceboxBackupConfig struct {
	SourceURI   string `json:"sourceUri"`
	AlbumID     string `json:"albumId"`
	WiFiOnly    bool   `json:"wifiOnly"`
	AutoEnabled bool   `json:"autoEnabled"`
}

type SourceboxExport struct {
	Record     SourceboxAction          `json:"record"`
	Comments   []SourceboxActionComment `json:"comments"`
	Checklists []SourceboxChecklist     `json:"checklists"`
	Other      json.RawMessage          `json:"other"`
	Habit      *SourceboxHabit          `json:"habit"`
}

type SourceboxPhotoExport struct {
	Albums      []SourceboxPhotoAlbum `json:"albums"`
	Photos      []SourceboxPhoto      `json:"photos"`
	SmartAlbums []SourceboxSmartAlbum `json:"smartAlbums"`
}

type SourceboxSnapshot struct {
	Actions         []SourceboxExport         `json:"actions"`
	Notes           []SourceboxNote           `json:"notes"`
	Commands        []SourceboxCommand        `json:"commands"`
	ActionTypes     []SourceboxActionType     `json:"actionTypes"`
	ActionTypeState string                    `json:"actionTypeStates"`
	Recommendations []SourceboxRecommendation `json:"recommendations"`
	Photos          SourceboxPhotoExport      `json:"photos"`
}

type SourceboxCounts struct {
	Actions         int `json:"actions"`
	Notes           int `json:"notes"`
	Todos           int `json:"todos"`
	Tasks           int `json:"tasks"`
	Books           int `json:"books"`
	Commands        int `json:"commands"`
	Comments        int `json:"comments"`
	Checklists      int `json:"checklists"`
	Habits          int `json:"habits"`
	ActionTypes     int `json:"actionTypes"`
	Recommendations int `json:"recommendations"`
	PhotoAlbums     int `json:"photoAlbums"`
	Photos          int `json:"photos"`
	SmartAlbums     int `json:"smartAlbums"`
}

type SourceboxActionSummary struct {
	ID       string `json:"id"`
	Name     string `json:"name"`
	Type     string `json:"type"`
	State    string `json:"state"`
	Category string `json:"category"`
	Remark   string `json:"remark"`
	Start    string `json:"start"`
	End      string `json:"end"`
	Duration int    `json:"duration"`
}

type SourceboxPhotoSummary struct {
	ID           string `json:"id"`
	AlbumID      string `json:"albumId"`
	FileName     string `json:"fileName"`
	RelativePath string `json:"relativePath"`
	Status       string `json:"status"`
	TakenAt      string `json:"takenAt"`
}

func (s *Store) SaveSourceboxSnapshot(snapshot SourceboxSnapshot) error {
	tx, err := s.db.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()

	tables := []string{
		"sourcebox_actions", "sourcebox_action_comments", "sourcebox_checklists",
		"sourcebox_habits", "sourcebox_notes", "sourcebox_action_types",
		"sourcebox_recommendations", "sourcebox_photo_albums", "sourcebox_photos",
		"sourcebox_smart_albums",
	}
	for _, table := range tables {
		if table == "sourcebox_notes" && snapshot.Notes == nil {
			continue
		}
		if _, err := tx.Exec("DELETE FROM " + table); err != nil {
			return err
		}
	}
	if snapshot.Commands != nil {
		if _, err := tx.Exec(`DELETE FROM sourcebox_commands`); err != nil {
			return err
		}
	}
	for _, exported := range snapshot.Actions {
		if err := insertAction(tx, exported); err != nil {
			return err
		}
	}
	for _, n := range snapshot.Notes {
		if _, err := tx.Exec(`INSERT INTO sourcebox_notes(id,content,date,state) VALUES(?,?,?,?)`,
			n.ID, n.Content, n.Date, n.State); err != nil {
			return err
		}
	}
	for _, command := range snapshot.Commands {
		tags := command.Tags
		if tags == nil {
			tags = []string{}
		}
		tagsJSON, err := json.Marshal(tags)
		if err != nil {
			return err
		}
		if _, err := tx.Exec(`INSERT INTO sourcebox_commands
(id,cmd,alias,tags_json,remark,frequency,create_time,update_time) VALUES(?,?,?,?,?,?,?,?)`,
			command.ID, command.Cmd, command.Alias, string(tagsJSON), command.Remark,
			command.Frequency, command.CreateTime, command.UpdateTime); err != nil {
			return err
		}
	}
	for _, actionType := range snapshot.ActionTypes {
		if _, err := tx.Exec(`INSERT INTO sourcebox_action_types(id,name,type,create_time) VALUES(?,?,?,?)`,
			actionType.ID, actionType.Name, actionType.Type, actionType.CreateTime); err != nil {
			return err
		}
	}
	for _, recommendation := range snapshot.Recommendations {
		if _, err := tx.Exec(`INSERT INTO sourcebox_recommendations(id,data_id,table_name,create_time,update_time)
VALUES(?,?,?,?,?)`, recommendation.ID, recommendation.DataID, recommendation.Table,
			recommendation.CreateTime, recommendation.UpdateTime); err != nil {
			return err
		}
	}
	if _, err := tx.Exec(`INSERT INTO sourcebox_metadata(key,value) VALUES('action_type_states',?)
ON CONFLICT(key) DO UPDATE SET value=excluded.value`, snapshot.ActionTypeState); err != nil {
		return err
	}
	for _, album := range snapshot.Photos.Albums {
		if _, err := tx.Exec(`INSERT INTO sourcebox_photo_albums
(id,name,type,source_roots,recursive,cover_photo_id,status,create_time,update_time)
VALUES(?,?,?,?,?,?,?,?,?)`, album.ID, album.Name, album.Type, album.SourceRoots, album.Recursive,
			album.CoverPhotoID, album.Status, album.CreateTime, album.UpdateTime); err != nil {
			return err
		}
	}
	for _, photo := range snapshot.Photos.Photos {
		if _, err := tx.Exec(`INSERT INTO sourcebox_photos
(id,album_id,file_name,relative_path,source_path,mime_type,sha256,size,width,height,taken_at,
metadata_status,metadata_json,status,trashed_at,create_time,update_time)
VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)`, photo.ID, photo.AlbumID, photo.FileName, photo.RelativePath,
			photo.SourcePath, photo.MimeType, photo.SHA256, photo.Size, photo.Width, photo.Height,
			photo.TakenAt, photo.MetadataStatus, photo.MetadataJSON, photo.Status, photo.TrashedAt,
			photo.CreateTime, photo.UpdateTime); err != nil {
			return err
		}
	}
	for _, album := range snapshot.Photos.SmartAlbums {
		if _, err := tx.Exec(`INSERT INTO sourcebox_smart_albums
(id,name,match_type,rules_json,enabled,create_time,update_time) VALUES(?,?,?,?,?,?,?)`,
			album.ID, album.Name, album.MatchType, album.RulesJSON, album.Enabled,
			album.CreateTime, album.UpdateTime); err != nil {
			return err
		}
	}
	return tx.Commit()
}

func insertAction(tx *sql.Tx, exported SourceboxExport) error {
	action := exported.Record
	comments, err := json.Marshal(action.Comments)
	if err != nil {
		return err
	}
	var other any
	if len(exported.Other) > 0 {
		other = string(exported.Other)
	}
	_, err = tx.Exec(`INSERT INTO sourcebox_actions
(id,name,icon,action_index,device,type,state,category,rating,comments_json,labels,refs,remark,start_at,end_at,
duration,create_time,update_time,other_json)
VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)`,
		action.ID, action.Name, action.Icon, action.Index, action.Device, action.Type, action.State,
		action.Category, action.Rating, string(comments), action.Labels, action.Refs, action.Remark,
		action.Start, action.End, action.Duration, action.CreateTime, action.UpdateTime, other)
	if err != nil {
		return err
	}
	for _, comment := range exported.Comments {
		if _, err := tx.Exec(`INSERT INTO sourcebox_action_comments(id,action_id,content,create_time)
VALUES(?,?,?,?)`, comment.ID, comment.ActionID, comment.Content, comment.CreateTime); err != nil {
			return err
		}
	}
	for _, checklist := range exported.Checklists {
		if _, err := tx.Exec(`INSERT INTO sourcebox_checklists
(id,action_id,content,first_value,last_value,current_value,create_time,update_time)
VALUES(?,?,?,?,?,?,?,?)`, checklist.ID, checklist.ActionID, checklist.Content, checklist.First,
			checklist.Last, checklist.Current, checklist.CreateTime, checklist.UpdateTime); err != nil {
			return err
		}
	}
	if habit := exported.Habit; habit != nil {
		if _, err := tx.Exec(`INSERT INTO sourcebox_habits
(id,category,action_id,note,unit,freq_num,freq_den,question,target_value,target_type,create_time,update_time)
VALUES(?,?,?,?,?,?,?,?,?,?,?,?)`, habit.ID, habit.Category, habit.ActionID, habit.Note, habit.Unit,
			habit.FreqNum, habit.FreqDen, habit.Question, habit.TargetValue, habit.TargetType,
			habit.CreateTime, habit.UpdateTime); err != nil {
			return err
		}
	}
	return nil
}

func (s *Store) SourceboxCounts() (SourceboxCounts, error) {
	var counts SourceboxCounts
	queries := []struct {
		query string
		dest  *int
	}{
		{"SELECT COUNT(*) FROM sourcebox_actions", &counts.Actions},
		{"SELECT COUNT(*) FROM sourcebox_notes", &counts.Notes},
		{"SELECT (SELECT COUNT(*) FROM sourcebox_todos) + (SELECT COUNT(*) FROM sourcebox_actions WHERE type='todo')", &counts.Todos},
		{"SELECT COUNT(*) FROM sourcebox_actions WHERE type='record'", &counts.Tasks},
		{"SELECT COUNT(*) FROM sourcebox_actions WHERE type='book'", &counts.Books},
		{"SELECT COUNT(*) FROM sourcebox_commands", &counts.Commands},
		{"SELECT COUNT(*) FROM sourcebox_action_comments", &counts.Comments},
		{"SELECT COUNT(*) FROM sourcebox_checklists", &counts.Checklists},
		{"SELECT COUNT(*) FROM sourcebox_habits", &counts.Habits},
		{"SELECT COUNT(*) FROM sourcebox_action_types", &counts.ActionTypes},
		{"SELECT COUNT(*) FROM sourcebox_recommendations", &counts.Recommendations},
		{"SELECT COUNT(*) FROM sourcebox_photo_albums", &counts.PhotoAlbums},
		{"SELECT COUNT(*) FROM sourcebox_photos", &counts.Photos},
		{"SELECT COUNT(*) FROM sourcebox_smart_albums", &counts.SmartAlbums},
	}
	for _, query := range queries {
		if err := s.db.QueryRow(query.query).Scan(query.dest); err != nil {
			return counts, fmt.Errorf("count local data: %w", err)
		}
	}
	return counts, nil
}

func (s *Store) ListSourceboxActions(actionType string, limit int) ([]SourceboxActionSummary, error) {
	if limit <= 0 || limit > 500 {
		limit = 100
	}
	rows, err := s.db.Query(`SELECT id,name,type,state,COALESCE(category,''),COALESCE(remark,''),
COALESCE(start_at,''),COALESCE(end_at,''),COALESCE(duration,0)
FROM sourcebox_actions WHERE (? = '' OR type = ?) ORDER BY COALESCE(start_at,create_time,'') DESC LIMIT ?`,
		actionType, actionType, limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := make([]SourceboxActionSummary, 0)
	for rows.Next() {
		var action SourceboxActionSummary
		if err := rows.Scan(&action.ID, &action.Name, &action.Type, &action.State, &action.Category,
			&action.Remark, &action.Start, &action.End, &action.Duration); err != nil {
			return nil, err
		}
		out = append(out, action)
	}
	return out, rows.Err()
}

func (s *Store) ListSourceboxNotes(limit int) ([]SourceboxNote, error) {
	if limit <= 0 || limit > 500 {
		limit = 100
	}
	rows, err := s.db.Query(`SELECT id,content,date,state FROM sourcebox_notes ORDER BY date DESC LIMIT ?`, limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := make([]SourceboxNote, 0)
	for rows.Next() {
		var note SourceboxNote
		if err := rows.Scan(&note.ID, &note.Content, &note.Date, &note.State); err != nil {
			return nil, err
		}
		out = append(out, note)
	}
	return out, rows.Err()
}

func (s *Store) ListSourceboxPhotos(limit int) ([]SourceboxPhotoSummary, error) {
	if limit <= 0 || limit > 500 {
		limit = 100
	}
	rows, err := s.db.Query(`SELECT id,album_id,file_name,relative_path,status,COALESCE(taken_at,'')
FROM sourcebox_photos ORDER BY COALESCE(taken_at,create_time,'') DESC LIMIT ?`, limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := make([]SourceboxPhotoSummary, 0)
	for rows.Next() {
		var photo SourceboxPhotoSummary
		if err := rows.Scan(&photo.ID, &photo.AlbumID, &photo.FileName, &photo.RelativePath,
			&photo.Status, &photo.TakenAt); err != nil {
			return nil, err
		}
		out = append(out, photo)
	}
	return out, rows.Err()
}
