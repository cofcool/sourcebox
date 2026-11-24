package service

import (
	"fmt"
	"net/http"
	"net/http/httptest"
	"path/filepath"
	"testing"

	"github.com/example/thesourcebox-desktop/internal/store"
)

func TestSyncSourceboxDataUsesSessionCredentialsAndPersistsExports(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		username, password, ok := r.BasicAuth()
		if !ok || username != "tester" || password != "secret" {
			http.Error(w, "missing Basic auth", http.StatusUnauthorized)
			return
		}
		w.Header().Set("Content-Type", "application/json")
		switch r.URL.Path {
		case "/action/exportAll":
			fmt.Fprint(w, `[{"record":{"id":"a1","name":"Read","type":"book","state":"done","comments":["nice"]},"comments":[],"checklists":[]}]`)
		case "/note/exportAll":
			fmt.Fprint(w, `[{"id":"n1","content":"memo","date":"2026-11-24 10:00:00","state":"NORMAL"}]`)
		case "/cmd/exportAll":
			fmt.Fprint(w, `[{"id":"c1","cmd":"git status","tags":["git"],"frequency":2}]`)
		case "/action/types":
			fmt.Fprint(w, `{"types":[{"id":"book","name":"Book","type":"record"}],"states":"todo,done"}`)
		case "/action/recommend":
			fmt.Fprint(w, `{"data":[{"id":"r1","dataId":"a1","table":"action"}]}`)
		case "/photo/exportAll":
			fmt.Fprint(w, `{"albums":[{"id":"p1","name":"Photos"}],"photos":[{"id":"f1","albumId":"p1","fileName":"one.jpg","size":12}],"smartAlbums":[]}`)
		default:
			http.NotFound(w, r)
		}
	}))
	defer server.Close()

	db, err := store.Open(filepath.Join(t.TempDir(), "data"))
	if err != nil {
		t.Fatal(err)
	}
	defer db.Close()
	services := New(db)
	if err := services.SetBackendCredentials(server.URL, "tester", "secret"); err != nil {
		t.Fatal(err)
	}
	counts, err := services.SyncSourceboxData()
	if err != nil {
		t.Fatal(err)
	}
	if counts.Actions != 1 || counts.Books != 1 || counts.Notes != 1 ||
		counts.Commands != 1 || counts.ActionTypes != 1 ||
		counts.Recommendations != 1 || counts.PhotoAlbums != 1 || counts.Photos != 1 {
		t.Fatalf("unexpected sync counts: %+v", counts)
	}
	actions, err := services.ListSourceboxActions("book", 10)
	if err != nil {
		t.Fatal(err)
	}
	if len(actions) != 1 || actions[0].Name != "Read" || actions[0].State != "done" {
		t.Fatalf("unexpected synced actions: %+v", actions)
	}
	if _, err := db.GetConfig("api.password"); err == nil {
		t.Fatal("backend password must not be persisted")
	}
}

func TestDecodeRecommendationsAcceptsSingleAndWrappedObjects(t *testing.T) {
	tests := []struct {
		name string
		body string
		want int
	}{
		{name: "single record", body: `{"id":"r1","data_id":"a1","table_name":"action"}`, want: 1},
		{name: "data envelope", body: `{"data":{"items":[{"id":"r1","dataId":"a1","table":"action"}]}}`, want: 1},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			recommendations, err := decodeRecommendations([]byte(test.body))
			if err != nil {
				t.Fatal(err)
			}
			if len(recommendations) != test.want {
				t.Fatalf("got %d recommendations, want %d", len(recommendations), test.want)
			}
			if recommendations[0].ID != "r1" || recommendations[0].DataID != "a1" ||
				recommendations[0].Table != "action" {
				t.Fatalf("unexpected recommendation: %+v", recommendations[0])
			}
		})
	}

	if _, err := decodeRecommendations([]byte(`{"message":"not recommendations"}`)); err == nil {
		t.Fatal("expected an unsupported response object to fail")
	}
}
