package service

import (
	"encoding/json"
	"fmt"
	"strings"

	"github.com/example/thesourcebox-desktop/internal/store"
)

func (s *Services) SyncSourceboxData() (store.SourceboxCounts, error) {
	var snapshot store.SourceboxSnapshot
	if err := s.getBackendJSON("/action/exportAll", &snapshot.Actions); err != nil {
		return store.SourceboxCounts{}, err
	}
	if err := s.getBackendJSON("/note/exportAll", &snapshot.Notes); err != nil {
		return store.SourceboxCounts{}, err
	}
	if err := s.getBackendJSON("/cmd/exportAll", &snapshot.Commands); err != nil {
		return store.SourceboxCounts{}, err
	}

	var actionTypes struct {
		Types  []store.SourceboxActionType `json:"types"`
		States string                      `json:"states"`
	}
	if err := s.getBackendJSON("/action/types", &actionTypes); err != nil {
		return store.SourceboxCounts{}, err
	}
	snapshot.ActionTypes = actionTypes.Types
	snapshot.ActionTypeState = actionTypes.States

	var recommendationResponse json.RawMessage
	if err := s.getBackendJSON("/action/recommend", &recommendationResponse); err != nil {
		return store.SourceboxCounts{}, err
	}
	recommendations, err := decodeRecommendations(recommendationResponse)
	if err != nil {
		return store.SourceboxCounts{}, fmt.Errorf("decode /action/recommend response: %w", err)
	}
	snapshot.Recommendations = recommendations
	if err := s.getBackendJSON("/photo/exportAll", &snapshot.Photos); err != nil {
		return store.SourceboxCounts{}, err
	}
	if err := s.store.SaveSourceboxSnapshot(snapshot); err != nil {
		return store.SourceboxCounts{}, fmt.Errorf("save synchronized data: %w", err)
	}
	return s.store.SourceboxCounts()
}

func decodeRecommendations(data []byte) ([]store.SourceboxRecommendation, error) {
	trimmed := strings.TrimSpace(string(data))
	if trimmed == "" || trimmed == "null" {
		return nil, fmt.Errorf("expected a recommendation array or object, got %s", trimmed)
	}
	switch trimmed[0] {
	case '[':
		var entries []json.RawMessage
		if err := json.Unmarshal(data, &entries); err != nil {
			return nil, err
		}
		recommendations := make([]store.SourceboxRecommendation, 0, len(entries))
		for i, entry := range entries {
			recommendation, err := decodeRecommendation(entry)
			if err != nil {
				return nil, fmt.Errorf("recommendation %d: %w", i, err)
			}
			recommendations = append(recommendations, recommendation)
		}
		return recommendations, nil
	case '{':
		var object map[string]json.RawMessage
		if err := json.Unmarshal(data, &object); err != nil {
			return nil, err
		}
		for _, key := range []string{"recommendations", "recommendation", "recommend", "data", "items", "records", "results", "result", "list"} {
			if value, ok := object[key]; ok {
				recommendations, err := decodeRecommendations(value)
				if err == nil {
					return recommendations, nil
				}
			}
		}
		recommendation, err := decodeRecommendation(data)
		if err != nil {
			return nil, fmt.Errorf("unsupported recommendation object: %w", err)
		}
		return []store.SourceboxRecommendation{recommendation}, nil
	default:
		return nil, fmt.Errorf("expected a recommendation array or object, got %s", trimmed[:1])
	}
}

func decodeRecommendation(data []byte) (store.SourceboxRecommendation, error) {
	var fields map[string]json.RawMessage
	if err := json.Unmarshal(data, &fields); err != nil {
		return store.SourceboxRecommendation{}, err
	}
	readString := func(keys ...string) string {
		for _, key := range keys {
			var value string
			if raw, ok := fields[key]; ok && json.Unmarshal(raw, &value) == nil {
				return value
			}
		}
		return ""
	}
	recommendation := store.SourceboxRecommendation{
		ID:     readString("id"),
		DataID: readString("dataId", "data_id"),
		Table:  readString("table", "tableName", "table_name"),
	}
	if recommendation.ID == "" || recommendation.DataID == "" || recommendation.Table == "" {
		return store.SourceboxRecommendation{}, fmt.Errorf("missing required id, dataId, or table field")
	}
	if raw, ok := fields["createTime"]; ok {
		if err := json.Unmarshal(raw, &recommendation.CreateTime); err != nil {
			return store.SourceboxRecommendation{}, fmt.Errorf("invalid createTime: %w", err)
		}
	} else if raw, ok := fields["create_time"]; ok {
		if err := json.Unmarshal(raw, &recommendation.CreateTime); err != nil {
			return store.SourceboxRecommendation{}, fmt.Errorf("invalid create_time: %w", err)
		}
	}
	if raw, ok := fields["updateTime"]; ok {
		if err := json.Unmarshal(raw, &recommendation.UpdateTime); err != nil {
			return store.SourceboxRecommendation{}, fmt.Errorf("invalid updateTime: %w", err)
		}
	} else if raw, ok := fields["update_time"]; ok {
		if err := json.Unmarshal(raw, &recommendation.UpdateTime); err != nil {
			return store.SourceboxRecommendation{}, fmt.Errorf("invalid update_time: %w", err)
		}
	}
	return recommendation, nil
}

func (s *Services) SourceboxCounts() (store.SourceboxCounts, error) {
	return s.store.SourceboxCounts()
}

func (s *Services) ListSourceboxActions(actionType string, limit int) ([]store.SourceboxActionSummary, error) {
	return s.store.ListSourceboxActions(actionType, limit)
}

func (s *Services) ListSourceboxNotes(limit int) ([]store.SourceboxNote, error) {
	return s.store.ListSourceboxNotes(limit)
}

func (s *Services) ListSourceboxPhotos(limit int) ([]store.SourceboxPhotoSummary, error) {
	return s.store.ListSourceboxPhotos(limit)
}

func (s *Services) getBackendJSON(path string, target any) error {
	response, err := s.requestBackend("GET", path, "", 32<<20)
	if err != nil {
		return fmt.Errorf("request %s: %w", path, err)
	}
	if response.StatusCode < 200 || response.StatusCode >= 300 {
		body := strings.TrimSpace(response.Body)
		if len(body) > 300 {
			body = body[:300] + "..."
		}
		return fmt.Errorf("request %s returned HTTP %d: %s", path, response.StatusCode, body)
	}
	if err := json.Unmarshal([]byte(response.Body), target); err != nil {
		return fmt.Errorf("decode %s response: %w", path, err)
	}
	return nil
}
