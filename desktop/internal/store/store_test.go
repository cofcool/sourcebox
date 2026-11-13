package store

import (
	"path/filepath"
	"testing"
)

func TestStoreCommandRoundTrip(t *testing.T) {
	s, err := Open(filepath.Join(t.TempDir(), "data"))
	if err != nil { t.Fatal(err) }
	defer s.Close()
	c := Command{ID:"test.custom", Name:"Echo", Kind:"custom", Executable:"echo", Args:[]string{"hello"}, TimeoutSeconds:5}
	if err := s.SaveCommand(c); err != nil { t.Fatal(err) }
	got, err := s.GetCommand(c.ID)
	if err != nil { t.Fatal(err) }
	if len(got.Args) != 1 || got.Args[0] != "hello" { t.Fatalf("unexpected args: %#v", got.Args) }
}
