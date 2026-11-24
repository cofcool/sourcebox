package main

import (
	"log"

	"github.com/egoist/mygo"
	"github.com/egoist/mygo/ui"

	"github.com/example/thesourcebox-desktop/internal/app"
	"github.com/example/thesourcebox-desktop/internal/desktop"
	"github.com/example/thesourcebox-desktop/internal/service"
	"github.com/example/thesourcebox-desktop/internal/store"
)

func main() {
	dataDir, err := app.DataDir()
	if err != nil {
		log.Fatal(err)
	}
	db, err := store.Open(dataDir)
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	services := service.New(db)
	workspace := desktop.NewWorkspace(services)
	mygo.App.WhenReady(func() {
		mygo.NewWindow(mygo.WindowOptions{
			Title:     "TheSourceBox",
			Width:     1180,
			Height:    780,
			MinWidth:  850,
			MinHeight: 560,
			StateKey:  "main",
			Content:   ui.View(workspace.View),
		})

		desktop.RegisterSystemIntegration()
	})

	if err := mygo.App.Run(); err != nil {
		log.Fatal(err)
	}
}
