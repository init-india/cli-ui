"""
Helper functions and utilities
"""

import os
import shutil

def format_file_size(size_bytes: int) -> str:
    """Format file size in human readable format"""
    if size_bytes == 0:
        return "0B"
    
    size_names = ["B", "KB", "MB", "GB", "TB"]
    i = 0
    while size_bytes >= 1024 and i < len(size_names) - 1:
        size_bytes /= 1024.0
        i += 1
    
    return f"{size_bytes:.1f} {size_names[i]}"

def get_file_icon(filename: str) -> str:
    """Get appropriate icon for file type"""
    if os.path.isdir(filename):
        return "📁"
    
    ext = os.path.splitext(filename)[1].lower()
    icons = {
        '.txt': '📄', '.pdf': '📕', '.doc': '📘', '.docx': '📘',
        '.jpg': '🖼', '.jpeg': '🖼', '.png': '🖼', '.gif': '🖼',
        '.mp3': '🎵', '.mp4': '🎬', '.avi': '🎬', '.mkv': '🎬',
        '.zip': '📦', '.rar': '📦', '.7z': '📦',
        '.py': '🐍', '.js': '📜', '.html': '🌐', '.css': '🎨',
    }
    
    return icons.get(ext, '📄')
