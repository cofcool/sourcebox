package desktop

import (
	"fmt"

	"github.com/egoist/mygo/ui"
	"github.com/example/thesourcebox-desktop/internal/service"
)

type Launcher struct {
	services *service.Services
	last     string
}

func NewLauncher(s *service.Services) *Launcher { return &Launcher{services: s} }

func (l *Launcher) View(c *ui.Context) {
	ui.Column(c).Fill().Padding(20).Gap(12).Children(func() {
		ui.Text(c, "TheSourceBox").FontSize(24).Bold()
		ui.Text(c, "Quick launcher").FontSize(13)
		ui.Text(c, "Built-in commands").FontSize(16).Bold()

		if ui.PrimaryButton(c, "System information").Clicked() {
			r, err := l.services.RunCommand("system.info")
			if err != nil { l.last = err.Error() } else { l.last = r.Stdout }
		}
		if ui.Button(c, "Current directory").Clicked() {
			r, err := l.services.RunCommand("system.cwd")
			if err != nil { l.last = err.Error() } else { l.last = r.Stdout }
		}
		if ui.Button(c, "Git status").Clicked() {
			r, err := l.services.RunCommand("git.status")
			if err != nil { l.last = err.Error() } else { l.last = r.Stdout + r.Stderr }
		}
		ui.Text(c, "Output").FontSize(14).Bold()
		if l.last == "" { l.last = "Choose a command to run." }
		ui.Text(c, fmt.Sprintf("%s", l.last))
	})
}
