package service

import (
	"bytes"
	"errors"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"
	"sync"
	"time"

	"github.com/example/thesourcebox-desktop/internal/store"
)

type Services struct {
	store    *store.Store
	mu       sync.RWMutex
	client   *http.Client
	baseURL  string
	token    string // intentionally memory-only; use OS keychain before production
	username string
	password string
}

func New(db *store.Store) *Services {
	s := &Services{
		store:  db,
		client: &http.Client{Timeout: 30 * time.Second},
	}
	if v, err := db.GetConfig("api.base_url"); err == nil {
		s.baseURL = v
	}
	return s
}

func (s *Services) ListCommands() ([]store.Command, error)         { return s.store.ListCommands() }
func (s *Services) ClearBusinessData() error                       { return s.store.ClearBusinessData() }
func (s *Services) ListHistory(limit int) ([]store.History, error) { return s.store.ListHistory(limit) }
func (s *Services) DeleteCustomCommand(id string) error            { return s.store.DeleteCustomCommand(id) }

func (s *Services) SetBackend(baseURL, token string) error {
	baseURL = strings.TrimSpace(baseURL)
	if baseURL == "" {
		if err := s.store.DeleteConfig("api.base_url"); err != nil {
			return err
		}
		s.mu.Lock()
		s.baseURL, s.token, s.username, s.password = "", "", "", ""
		s.mu.Unlock()
		return nil
	}
	u, err := url.Parse(baseURL)
	if err != nil || (u.Scheme != "http" && u.Scheme != "https") || u.Host == "" {
		return errors.New("backend URL must be a valid http or https URL")
	}
	if u.User != nil {
		return errors.New("backend URL must not contain credentials")
	}
	baseURL = strings.TrimRight(u.String(), "/")
	if err := s.store.SetConfig("api.base_url", baseURL); err != nil {
		return err
	}
	s.mu.Lock()
	s.baseURL, s.token = baseURL, token
	s.username, s.password = "", ""
	s.mu.Unlock()
	return nil
}

// SetBackendCredentials configures Android-compatible Basic authentication for this session.
func (s *Services) SetBackendCredentials(baseURL, username, password string) error {
	if err := s.SetBackend(baseURL, ""); err != nil {
		return err
	}
	if strings.TrimSpace(baseURL) == "" {
		return nil
	}
	s.mu.Lock()
	s.username, s.password = strings.TrimSpace(username), password
	s.mu.Unlock()
	return nil
}

// SetBackendURL changes only the persistent base URL and preserves the in-memory token.
func (s *Services) SetBackendURL(baseURL string) error {
	s.mu.RLock()
	token, username, password := s.token, s.username, s.password
	s.mu.RUnlock()
	if err := s.SetBackend(baseURL, token); err != nil {
		return err
	}
	if strings.TrimSpace(baseURL) != "" && username != "" {
		s.mu.Lock()
		s.username, s.password = username, password
		s.mu.Unlock()
	}
	return nil
}

func (s *Services) BackendURL() string {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.baseURL
}

type HTTPResult struct {
	StatusCode int    `json:"statusCode"`
	Body       string `json:"body"`
}

func (s *Services) RequestBackend(method, path, body string) (HTTPResult, error) {
	return s.requestBackend(method, path, body, 2<<20)
}

func (s *Services) requestBackend(method, path, body string, maxResponseSize int64) (HTTPResult, error) {
	method = strings.ToUpper(strings.TrimSpace(method))
	switch method {
	case http.MethodGet, http.MethodPost, http.MethodPut, http.MethodPatch, http.MethodDelete:
	default:
		return HTTPResult{}, errors.New("unsupported HTTP method")
	}

	s.mu.RLock()
	baseURL, token, username, password := s.baseURL, s.token, s.username, s.password
	s.mu.RUnlock()
	if baseURL == "" {
		return HTTPResult{}, errors.New("backend URL is not configured")
	}
	base, _ := url.Parse(baseURL + "/")
	rel, err := url.Parse(strings.TrimLeft(path, "/"))
	if err != nil {
		return HTTPResult{}, err
	}
	target := base.ResolveReference(rel)

	var reqBody io.Reader
	if body != "" {
		reqBody = bytes.NewBufferString(body)
	}
	req, err := http.NewRequest(method, target.String(), reqBody)
	if err != nil {
		return HTTPResult{}, err
	}
	if body != "" {
		req.Header.Set("Content-Type", "application/json")
	}
	if token != "" {
		req.Header.Set("Authorization", "Bearer "+token)
	}
	if username != "" {
		req.SetBasicAuth(username, password)
	}
	resp, err := s.client.Do(req)
	if err != nil {
		return HTTPResult{}, err
	}
	defer resp.Body.Close()
	data, err := io.ReadAll(io.LimitReader(resp.Body, maxResponseSize+1))
	if err != nil {
		return HTTPResult{}, err
	}
	if int64(len(data)) > maxResponseSize {
		return HTTPResult{}, fmt.Errorf("backend response exceeds %d MiB", maxResponseSize>>20)
	}
	return HTTPResult{StatusCode: resp.StatusCode, Body: string(data)}, nil
}
