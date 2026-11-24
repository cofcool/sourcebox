package desktop

import (
	"github.com/example/thesourcebox-desktop/internal/service"
	"github.com/example/thesourcebox-desktop/internal/store"
)

type API struct{ services *service.Services }

func NewAPI(s *service.Services) *API { return &API{services: s} }

func (a *API) ListCommands() ([]store.Command, error) {
	return a.services.ListCommands()
}

func (a *API) CreateCustomCommand(name, executable, argsJSON, workingDir string, timeoutSeconds int) (store.Command, error) {
	return a.services.CreateCustomCommand(name, executable, argsJSON, workingDir, timeoutSeconds)
}

func (a *API) DeleteCustomCommand(id string) error {
	return a.services.DeleteCustomCommand(id)
}

func (a *API) RunCommand(id string) (service.CommandResult, error) {
	return a.services.RunCommand(id)
}

func (a *API) ListHistory(limit int) ([]store.History, error) {
	return a.services.ListHistory(limit)
}

func (a *API) BackendURL() string { return a.services.BackendURL() }

func (a *API) SetBackend(baseURL, token string) error {
	return a.services.SetBackend(baseURL, token)
}

func (a *API) SetBackendCredentials(baseURL, username, password string) error {
	return a.services.SetBackendCredentials(baseURL, username, password)
}

func (a *API) SyncSourceboxData() (store.SourceboxCounts, error) {
	return a.services.SyncSourceboxData()
}

func (a *API) SourceboxCounts() (store.SourceboxCounts, error) {
	return a.services.SourceboxCounts()
}

func (a *API) RequestBackend(method, path, body string) (service.HTTPResult, error) {
	return a.services.RequestBackend(method, path, body)
}
