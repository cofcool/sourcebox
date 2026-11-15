package desktop

import (
	"github.com/egoist/mygo/ui"
	"github.com/example/thesourcebox-desktop/internal/service"
)

// Settings is the state and native view for the application settings window.
type Settings struct {
	services     *service.Services
	backendURL   string
	message      string
	confirmClear bool
}

func NewSettings(s *service.Services) *Settings {
	return &Settings{services: s, backendURL: s.BackendURL()}
}

func (s *Settings) View(c *ui.Context) {
	ui.Column(c).Fill().Padding(24).Gap(14).Children(func() {
		ui.Text(c, "Settings").FontSize(24).Bold()
		ui.Text(c, "Backend service").FontSize(16).Bold()
		ui.Text(c, "Configure the HTTP API base URL used by the application.").FontSize(13)
		ui.TextInput(c, &s.backendURL).Placeholder("https://api.example.com").Label("Backend URL").Width(420)
		if ui.PrimaryButton(c, "Save URL").Clicked() {
			if err := s.services.SetBackendURL(s.backendURL); err != nil {
				s.message = "Save failed: " + err.Error()
			} else if s.backendURL == "" {
				s.message = "Backend URL cleared."
			} else {
				s.message = "Backend URL saved."
			}
			s.confirmClear = false
		}
		if s.message != "" {
			ui.Text(c, s.message).FontSize(13)
		}
		ui.Divider(c)
		ui.Text(c, "Local database").FontSize(16).Bold()
		ui.Text(c, "Remove custom commands and execution history. The backend URL and built-in commands will be kept.").FontSize(13)
		if !s.confirmClear {
			if ui.Button(c, "Clear local data").Clicked() {
				s.confirmClear = true
				s.message = "Confirm clearing local business data? This cannot be undone."
			}
		} else {
			if ui.PrimaryButton(c, "Confirm clear").Clicked() {
				if err := s.services.ClearBusinessData(); err != nil {
					s.message = "Clear failed: " + err.Error()
				} else {
					s.message = "Local data cleared."
				}
				s.confirmClear = false
			}
			if ui.Button(c, "Cancel").Clicked() {
				s.confirmClear = false
				s.message = "Clear cancelled."
			}
		}
		ui.Text(c, "Database settings are stored locally on this device.").FontSize(12)
	})
}
