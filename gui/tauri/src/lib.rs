use std::{
    path::PathBuf,
    process::{Child, Command},
    sync::{Arc, Mutex},
    thread,
    time::Duration,
};

use tauri::Manager;

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
        if std::net::TcpStream::connect("127.0.0.1:47831").is_ok() {
            return;
        }
        thread::sleep(Duration::from_millis(100));
    }
}

pub fn run() {
    tauri::Builder::default()
        .setup(|app| {
            let child = start_backend(&app.handle())?;
            let process = Arc::new(Mutex::new(Some(child)));

            app.manage(BackendProcess(process.clone()));

            thread::spawn(move || {
                wait_for_backend();
            });

            Ok(())
        })
        .on_window_event(|window, event| {
            if let tauri::WindowEvent::CloseRequested { .. } = event {
                if let Some(state) = window.app_handle().try_state::<BackendProcess>() {
                    if let Ok(mut guard) = state.0.lock() {
                        if let Some(mut child) = guard.take() {
                            let _ = child.kill();
                        }
                    }
                }
            }
        })
        .plugin(tauri_plugin_shell::init())
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
