use std::{
    path::PathBuf,
    process::{Child, Command},
    sync::{Arc, Mutex},
    thread,
    time::Duration,
};

use tauri::{
    menu::{Menu, MenuItem},
    tray::TrayIconBuilder,
    Manager,
};

struct BackendProcess(Arc<Mutex<Option<Child>>>);

fn backend_jar(app: &tauri::AppHandle) -> Result<PathBuf, String> {
    let resource_dir = app.path().resource_dir().map_err(|e| e.to_string())?;
    Ok(resource_dir.join("resources").join("backend").join("sourcebox-fat.jar"))
}

fn java_command() -> String {
    if cfg!(target_os = "windows") {
        "java.exe".to_string()
    } else {
        "java".to_string()
    }
}

fn start_backend(app: &tauri::AppHandle) -> Result<Child, String> {
    let jar = backend_jar(app)?;
    if !jar.exists() {
        return Err(format!("Backend JAR not found: {}", jar.display()));
    }

    Command::new(java_command())
        .arg("-jar")
        .arg(jar)
        .spawn()
        .map_err(|e| format!("Failed to start Java backend: {e}"))
}

fn wait_for_backend() {
    // Development skeleton: Java listens on 47831.
    // Production should allocate a free port and pass --server.port=<port>.
    for _ in 0..100 {
        if std::net::TcpStream::connect("127.0.0.1:38080").is_ok() {
            return;
        }
        thread::sleep(Duration::from_millis(100));
    }
}

#[tauri::command]
fn set_tray_timer(app: tauri::AppHandle, text: String) -> Result<(), String> {
    let tray = app
        .tray_by_id("main")
        .ok_or_else(|| "Tray icon is unavailable".to_string())?;
    tray.set_tooltip(Some(text)).map_err(|e| e.to_string())
}

pub fn run() {
    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![set_tray_timer])
        .setup(|app| {
            // let child = start_backend(&app.handle())?;
            // let process = Arc::new(Mutex::new(Some(child)));

            // app.manage(BackendProcess(process.clone()));

            thread::spawn(move || {
                wait_for_backend();
            });

            let show = MenuItem::with_id(app, "show", "Show TheSourceBox", true, None::<&str>)?;
            let quit = MenuItem::with_id(app, "quit", "Quit", true, None::<&str>)?;
            let menu = Menu::with_items(app, &[&show, &quit])?;
            let icon = tauri::image::Image::from_bytes(include_bytes!("../icons/icon.png"))?;

            TrayIconBuilder::with_id("main")
                .icon(icon)
                .tooltip("Timer: stopped")
                .menu(&menu)
                .show_menu_on_left_click(false)
                .on_menu_event(|app, event| match event.id().as_ref() {
                    "show" => {
                        if let Some(window) = app.get_webview_window("main") {
                            let _ = window.show();
                            let _ = window.set_focus();
                        }
                    }
                    "quit" => app.exit(0),
                    _ => {}
                })
                .build(app)?;

            Ok(())
        })
        .on_window_event(|window, event| {
            if let tauri::WindowEvent::CloseRequested { api, .. } = event {
                api.prevent_close();
                let _ = window.hide();
            }
        })
        .plugin(tauri_plugin_shell::init())
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
