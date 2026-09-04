use rust_tool::{Args, LinkCovertTool, Tool};
use std::env;

fn main() {
    let mut it = env::args().skip(1);
    let items: Vec<String> = it.collect();
    if items.is_empty() {
        eprintln!("usage: rust-tool --input=FILE --output=FILE");
        return;
    }

    let args = Args::from_args(items);
    let tool = LinkCovertTool;
    if let Err(e) = tool.run(args) {
        eprintln!("error: {}", e);
    }
}
