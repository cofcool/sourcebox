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
	api := desktop.NewAPI(services)
	mygo.Bind(api)

	launcher := desktop.NewLauncher(services)
	settings := desktop.NewSettings(services)
	mygo.App.WhenReady(func() {
		mygo.NewWindow(mygo.WindowOptions{
			Title:     "TheSourceBox",
			URL:       "/",
			Width:     1180,
			Height:    780,
			MinWidth:  850,
			MinHeight: 560,
			StateKey:  "main",
		})

		mygo.NewWindow(mygo.WindowOptions{
			Title:     "TheSourceBox Settings",
			Width:     560,
			Height:    520,
			MinWidth:  480,
			MinHeight: 440,
			StateKey:  "settings",
			Content:   ui.View(settings.View),
		})

		mygo.NewWindow(mygo.WindowOptions{
			Title:     "TheSourceBox Launcher",
			Width:     420,
			Height:    520,
			MinWidth:  360,
			MinHeight: 420,
			StateKey:  "launcher",
			Content:   ui.View(launcher.View),
		})

		desktop.RegisterSystemIntegration()
	})

	if err := mygo.App.Run(); err != nil {
		log.Fatal(err)
	}
}
