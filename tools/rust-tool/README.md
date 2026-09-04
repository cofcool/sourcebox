# Rust Tool (port of Tool.java)

This crate contains a lightweight Rust port of the Java Tool/Args/Arg utilities found in the repository.

It includes:
- Tool/Args/Arg structures
- A simplified LinkCovertTool that extracts links from `.desktop` and `.webloc` files and appends Markdown links to an output file.

Usage (from repository root):

    cargo run -p rust_tool -- --input=demo.desktop --output=out.md

