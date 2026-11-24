package desktop

import (
	"fmt"
	"time"

	"github.com/egoist/mygo/ui"
	"github.com/example/thesourcebox-desktop/internal/service"
	"github.com/example/thesourcebox-desktop/internal/store"
)

type Workspace struct {
	services   *service.Services
	page       string
	message    string
	lastRun    string
	backend    string
	username   string
	password   string
	testPath   string
	name       string
	executable string
	argsJSON   string
	workingDir string
	timeout    string
	commands   []store.Command
	history    []store.History
	counts     store.SourceboxCounts
	actions    []store.SourceboxActionSummary
	notes      []store.SourceboxNote
	photos     []store.SourceboxPhotoSummary
}

func NewWorkspace(services *service.Services) *Workspace {
	w := &Workspace{
		services: services,
		page:     "today",
		backend:  services.BackendURL(),
		testPath: "/health",
		argsJSON: "[]",
		timeout:  "30",
	}
	w.refresh()
	return w
}

func (w *Workspace) View(c *ui.Context) {
	ui.Row(c).Fill().Children(func() {
		ui.Sidebar(c, &w.page, func() {
			ui.SidebarSection(c, "工作空间", nil, func() {
				ui.SidebarItem(c, "today", nil, "今日")
				ui.SidebarItem(c, "todos", nil, "待办")
				ui.SidebarItem(c, "habits", nil, "习惯")
				ui.SidebarItem(c, "tasks", nil, "任务")
			})
			ui.SidebarSection(c, "更多", nil, func() {
				ui.SidebarItem(c, "notes", nil, "便笺")
				ui.SidebarItem(c, "books", nil, "书籍")
				ui.SidebarItem(c, "photos", nil, "照片")
				ui.SidebarItem(c, "server-tools", nil, "服务端工具")
			})
			ui.SidebarSection(c, "桌面工具", nil, func() {
				ui.SidebarItem(c, "commands", nil, "本地命令")
				ui.SidebarItem(c, "history", nil, "执行历史")
				ui.SidebarItem(c, "settings", nil, "设置")
			})
		}).Width(230).FillHeight()

		ui.Column(c).Fill().Padding(28).Gap(16).Children(func() {
			ui.Row(c).FillWidth().AlignItems(ui.Center).Gap(12).Children(func() {
				ui.Column(c).Grow(1).Gap(4).Children(func() {
					ui.Text(c, "TheSourceBox").FontSize(24).Bold()
					ui.Text(c, w.pageTitle()).FontSize(13)
				})
				if ui.Button(c, "同步").Clicked() {
					w.sync()
				}
			})
			if w.message != "" {
				ui.Text(c, w.message).FontSize(13)
			}
			ui.Scroll(c).Fill().Children(func() {
				ui.Column(c).FillWidth().Gap(16).Children(func() {
					switch w.page {
					case "today":
						w.today(c)
					case "todos", "habits", "tasks", "notes", "books", "photos", "server-tools":
						w.module(c)
					case "commands":
						w.commandsPage(c)
					case "history":
						w.historyPage(c)
					case "settings":
						w.settingsPage(c)
					default:
						w.page = "today"
						w.today(c)
					}
				})
			})
		})
	})
}

func (w *Workspace) pageTitle() string {
	switch w.page {
	case "today":
		return "今日 · 概览"
	case "todos":
		return "待办"
	case "habits":
		return "习惯"
	case "tasks":
		return "任务"
	case "notes":
		return "便笺"
	case "books":
		return "书籍"
	case "photos":
		return "照片与相册"
	case "server-tools":
		return "服务端工具"
	case "commands":
		return "本地命令"
	case "history":
		return "执行历史"
	case "settings":
		return "连接与本地数据设置"
	default:
		return "今日 · 概览"
	}
}

func (w *Workspace) today(c *ui.Context) {
	ui.Row(c).FillWidth().Gap(12).Children(func() {
		w.metric(c, "行动记录", w.counts.Actions, "本地同步缓存")
		w.metric(c, "习惯", w.counts.Habits, "习惯定义")
		w.metric(c, "照片", w.counts.Photos, fmt.Sprintf("%d 个相册", w.counts.PhotoAlbums))
	})
	w.showActions(c, "")
	w.panel(c, "快速操作", "保持 Android 端的主要入口，同时提供桌面本地工具。", func() {
		ui.Row(c).Gap(10).Children(func() {
			if ui.PrimaryButton(c, "同步行动和照片").Clicked() {
				w.sync()
			}
			if ui.Button(c, "打开本地命令").Clicked() {
				w.page = "commands"
			}
			if ui.Button(c, "连接设置").Clicked() {
				w.page = "settings"
			}
		})
	})
	w.panel(c, "本地命令", "命令和执行历史保存在 SQLite 中。", func() {
		if len(w.commands) == 0 {
			ui.Text(c, "暂无可用命令。").FontSize(13)
		}
		for _, command := range w.commands {
			ui.Row(c).FillWidth().Gap(10).AlignItems(ui.Center).Children(func() {
				ui.Text(c, command.Name).Grow(1)
				if ui.Button(c, "运行").Clicked() {
					w.runCommand(command.ID)
				}
			})
		}
		if w.lastRun != "" {
			ui.TextArea(c, &w.lastRun).ReadOnly(true).Height(140).FillWidth()
		}
	})
}

func (w *Workspace) module(c *ui.Context) {
	var count int
	var description string
	switch w.page {
	case "todos":
		count = w.counts.Todos
		description = "待办和其他行动使用 Android 同源的行动字段，并在导出同步时写入 SQLite。"
	case "habits":
		count = w.counts.Habits
		description = "习惯定义、频率、单位、目标值和打卡清单以独立字段保存。"
	case "tasks":
		count = w.counts.Tasks
		description = "行动名称、类型、状态、分类、标签、时间、评分、评论和清单均有对应列。"
	case "notes":
		count = w.counts.Notes
		description = "便笺表映射 id、content、date、state 字段。"
	case "books":
		count = w.counts.Books
		description = "书籍记录属于行动模型，可在同步的行动记录中按 type 区分。"
	case "photos":
		count = w.counts.Photos
		description = "照片、相册和智能相册使用完整字段映射的 SQLite 表保存。"
	case "server-tools":
		count = w.counts.ActionTypes
		description = "连接到 Sourcebox 后端后，同步行动、行动类型、推荐及照片数据。"
	}
	ui.Text(c, description).FontSize(14)
	w.panel(c, "本地数据", "SQLite 缓存", func() {
		ui.Text(c, fmt.Sprintf("%s：%d", w.pageTitle(), count)).FontSize(20).Bold()
		ui.Text(c, fmt.Sprintf(
			"行动 %d  ·  待办 %d  ·  习惯 %d  ·  便笺 %d  ·  照片 %d  ·  相册 %d",
			w.counts.Actions, w.counts.Todos, w.counts.Habits,
			w.counts.Notes, w.counts.Photos, w.counts.PhotoAlbums,
		)).FontSize(13)
		if w.services.BackendURL() == "" {
			ui.Text(c, "请先在设置中配置后端根地址。").FontSize(12)
		}
		if ui.PrimaryButton(c, "同步").Clicked() {
			w.sync()
		}
	})
	switch w.page {
	case "todos":
		w.showActions(c, "todo")
	case "habits":
		w.showActions(c, "habit")
	case "tasks":
		w.showActions(c, "record")
	case "books":
		w.showActions(c, "book")
	case "notes":
		w.panel(c, "便笺列表", "", func() {
			if len(w.notes) == 0 {
				ui.Text(c, "本地暂无便笺记录。").FontSize(13)
			}
			for _, note := range w.notes {
				ui.Column(c).FillWidth().Gap(4).Padding(8).Children(func() {
					ui.Text(c, note.Content).FontSize(14)
					date := ""
					if note.Date != nil {
						date = *note.Date
					}
					ui.Text(c, date).FontSize(11)
				})
			}
		})
	case "photos":
		w.panel(c, "照片列表", "", func() {
			if len(w.photos) == 0 {
				ui.Text(c, "本地暂无照片记录。").FontSize(13)
			}
			for _, photo := range w.photos {
				ui.Row(c).FillWidth().Gap(10).Children(func() {
					ui.Text(c, photo.FileName).Grow(1)
					ui.Text(c, photo.RelativePath).FontSize(12)
					ui.Text(c, photo.Status).FontSize(12)
				})
			}
		})
	}
}

func (w *Workspace) showActions(c *ui.Context, actionType string) {
	actions := make([]store.SourceboxActionSummary, 0)
	for _, action := range w.actions {
		if actionType == "" || action.Type == actionType {
			actions = append(actions, action)
		}
		if len(actions) == 20 {
			break
		}
	}
	if len(actions) == 0 {
		return
	}
	w.panel(c, "行动记录", "最近同步的 Android 行动", func() {
		for _, action := range actions {
			ui.Row(c).FillWidth().Gap(10).AlignItems(ui.Center).Children(func() {
				ui.Column(c).Grow(1).Gap(3).Children(func() {
					ui.Text(c, action.Name).Bold()
					if action.Remark != "" {
						ui.Text(c, action.Remark).FontSize(12)
					}
				})
				ui.Text(c, action.State).FontSize(12)
				ui.Text(c, action.Start).FontSize(12)
			})
		}
	})
}

func (w *Workspace) commandsPage(c *ui.Context) {
	w.panel(c, "已注册命令", "执行时参数会分别传递，不经过 shell 拼接。", func() {
		for _, command := range w.commands {
			ui.Row(c).FillWidth().Gap(10).AlignItems(ui.Center).Children(func() {
				ui.Column(c).Grow(1).Gap(3).Children(func() {
					ui.Text(c, command.Name).Bold()
					if command.Kind == "custom" {
						ui.Text(c, command.Executable).FontSize(12)
					} else {
						ui.Text(c, "内置命令").FontSize(12)
					}
				})
				if ui.PrimaryButton(c, "运行").Clicked() {
					w.runCommand(command.ID)
				}
				if command.Kind == "custom" && ui.Button(c, "删除").Clicked() {
					if err := w.services.DeleteCustomCommand(command.ID); err != nil {
						w.message = "删除失败：" + err.Error()
					} else {
						w.message = "自定义命令已删除。"
						w.refresh()
					}
				}
			})
		}
		if w.lastRun != "" {
			ui.TextArea(c, &w.lastRun).ReadOnly(true).Height(150).FillWidth()
		}
	})
	w.panel(c, "添加自定义命令", "参数使用 JSON 字符串数组。", func() {
		ui.TextInput(c, &w.name).Placeholder("例如：检查状态").Label("名称").FillWidth()
		ui.TextInput(c, &w.executable).Placeholder("git 或 /usr/bin/python3").Label("可执行文件").FillWidth()
		ui.TextArea(c, &w.argsJSON).Label("参数 JSON 数组").Height(90).FillWidth()
		ui.TextInput(c, &w.workingDir).Placeholder("可选").Label("工作目录").FillWidth()
		ui.TextInput(c, &w.timeout).Placeholder("1–300").Label("超时（秒）").Width(180)
		if ui.PrimaryButton(c, "保存命令").Clicked() {
			w.createCommand()
		}
	})
}

func (w *Workspace) historyPage(c *ui.Context) {
	w.panel(c, "最近执行", "最多显示最近 50 条执行历史。", func() {
		if ui.Button(c, "刷新").Clicked() {
			w.refresh()
		}
		if len(w.history) == 0 {
			ui.Text(c, "暂无执行记录。").FontSize(13)
		}
		for _, item := range w.history {
			created, err := time.Parse(time.RFC3339Nano, item.CreatedAt)
			when := item.CreatedAt
			if err == nil {
				when = created.Local().Format("2006-01-02 15:04:05")
			}
			ui.Row(c).FillWidth().Gap(12).AlignItems(ui.Center).Children(func() {
				status := "失败"
				if item.ExitCode == 0 {
					status = "成功"
				}
				ui.Text(c, status).Width(52)
				ui.Text(c, item.Command).Grow(1)
				ui.Text(c, fmt.Sprintf("%d ms", item.DurationMS)).Width(72)
				ui.Text(c, when).Width(155)
			})
		}
	})
}

func (w *Workspace) settingsPage(c *ui.Context) {
	w.panel(c, "后端连接", "兼容 Android 的 Basic Auth；用户名和密码只保存在内存中。", func() {
		ui.TextInput(c, &w.backend).Placeholder("https://api.example.com").Label("后端根地址").FillWidth()
		ui.TextInput(c, &w.username).Placeholder("可选").Label("用户名").FillWidth()
		ui.TextInput(c, &w.password).Placeholder("可选").Label("密码").Password().FillWidth()
		if ui.PrimaryButton(c, "保存连接").Clicked() {
			if err := w.services.SetBackendCredentials(w.backend, w.username, w.password); err != nil {
				w.message = "保存失败：" + err.Error()
			} else {
				w.password = ""
				w.message = "连接设置已保存。密码仅在本次运行期间保留。"
			}
		}
		ui.Divider(c)
		ui.TextInput(c, &w.testPath).Placeholder("/health").Label("测试路径").FillWidth()
		if ui.Button(c, "发送 GET 请求").Clicked() {
			result, err := w.services.RequestBackend("GET", w.testPath, "")
			if err != nil {
				w.message = "请求失败：" + err.Error()
			} else {
				w.message = fmt.Sprintf("HTTP %d\n%s", result.StatusCode, result.Body)
			}
		}
	})
	w.panel(c, "SQLite 数据表", "已按 Android 数据模型字段建立本地表。", func() {
		ui.Text(c, fmt.Sprintf(
			"行动 %d · 评论 %d · 清单 %d · 习惯 %d · 行动类型 %d · 推荐 %d",
			w.counts.Actions, w.counts.Comments, w.counts.Checklists,
			w.counts.Habits, w.counts.ActionTypes, w.counts.Recommendations,
		)).FontSize(13)
		ui.Text(c, fmt.Sprintf(
			"便笺 %d · 待办 %d · 命令 %d · 照片 %d · 相册 %d · 智能相册 %d",
			w.counts.Notes, w.counts.Todos, w.counts.Commands,
			w.counts.Photos, w.counts.PhotoAlbums, w.counts.SmartAlbums,
		)).FontSize(13)
		if ui.PrimaryButton(c, "同步并保存缓存").Clicked() {
			w.sync()
		}
	})
}

func (w *Workspace) metric(c *ui.Context, title string, value int, detail string) {
	w.panel(c, title, detail, func() {
		ui.Text(c, fmt.Sprint(value)).FontSize(26).Bold()
	})
}

func (w *Workspace) panel(c *ui.Context, title, detail string, body func()) {
	ui.Column(c).FillWidth().Padding(16).Gap(10).
		Border(1, ui.RGB(210, 215, 225)).Radius(10).Children(func() {
		ui.Text(c, title).FontSize(16).Bold()
		if detail != "" {
			ui.Text(c, detail).FontSize(12)
		}
		body()
	})
}

func (w *Workspace) refresh() {
	commands, err := w.services.ListCommands()
	if err != nil {
		w.message = "读取命令失败：" + err.Error()
		return
	}
	history, err := w.services.ListHistory(50)
	if err != nil {
		w.message = "读取历史失败：" + err.Error()
		return
	}
	counts, err := w.services.SourceboxCounts()
	if err != nil {
		w.message = "读取 SQLite 缓存失败：" + err.Error()
		return
	}
	actions, err := w.services.ListSourceboxActions("", 100)
	if err != nil {
		w.message = "读取行动数据失败：" + err.Error()
		return
	}
	notes, err := w.services.ListSourceboxNotes(100)
	if err != nil {
		w.message = "读取便笺失败：" + err.Error()
		return
	}
	photos, err := w.services.ListSourceboxPhotos(100)
	if err != nil {
		w.message = "读取照片数据失败：" + err.Error()
		return
	}
	w.commands, w.history, w.counts = commands, history, counts
	w.actions, w.notes, w.photos = actions, notes, photos
	w.message = ""
}

func (w *Workspace) sync() {
	counts, err := w.services.SyncSourceboxData()
	if err != nil {
		w.message = "同步失败：" + err.Error()
		return
	}
	w.counts = counts
	w.refresh()
	w.message = fmt.Sprintf("同步完成：%d 条行动、%d 张照片、%d 个相册。", counts.Actions, counts.Photos, counts.PhotoAlbums)
}

func (w *Workspace) runCommand(id string) {
	result, err := w.services.RunCommand(id)
	if err != nil {
		w.message = "执行失败：" + err.Error()
		return
	}
	w.lastRun = fmt.Sprintf("%s · 退出码 %d · %d ms\n%s\n%s",
		result.Command, result.ExitCode, result.DurationMS, result.Stdout, result.Stderr)
	w.refresh()
}

func (w *Workspace) createCommand() {
	timeout := 30
	if _, err := fmt.Sscanf(w.timeout, "%d", &timeout); err != nil {
		w.message = "超时秒数必须是 1 到 300 之间的整数。"
		return
	}
	if _, err := w.services.CreateCustomCommand(w.name, w.executable, w.argsJSON, w.workingDir, timeout); err != nil {
		w.message = "保存命令失败：" + err.Error()
		return
	}
	w.name, w.executable, w.argsJSON, w.workingDir = "", "", "[]", ""
	w.message = "自定义命令已保存。"
	w.refresh()
}
