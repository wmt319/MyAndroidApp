package com.google.android.diskusage.filesystem.entity;

import androidx.annotation.NonNull;
import org.jetbrains.annotations.Contract;

public class FileSystemRoot extends FileSystemEntry {
  final String rootPath;
  private final boolean deletable;

  protected FileSystemRoot(String name, String rootPath, boolean deletable) {
    super(null, name);
    this.rootPath = rootPath;
    this.deletable = deletable;
  }
  
  @NonNull
  @Contract("_, _, _ -> new")
  public static FileSystemRoot makeNode(String name, String rootPath, boolean deletable) {
    return new FileSystemRoot(name, rootPath, deletable);
  }

  @Override
  public FileSystemEntry create() {
    return new FileSystemRoot(this.name, this.rootPath, this.deletable);
  }

  @Override
  public FileSystemEntry filter(CharSequence pattern, long blockSize) {
    // don't match name
    return filterChildren(pattern, blockSize);
  }

  public boolean isDeletable() {
    return deletable;
  }
  
  public static String withSlash(String path) {
    if (path.length() > 0 && path.charAt(path.length() - 1) != '/')
      path += '/';
    return path;
  }
  
  public final FileSystemEntry getByAbsolutePath(String path) {
    String rootPathWithSlash = withSlash(rootPath);
    String pathWithSlash = withSlash(path);
    
    if (pathWithSlash.equals(rootPathWithSlash)) {
      return getEntryByName(path, true);
    }
    if (pathWithSlash.startsWith(rootPathWithSlash)) {
      return getEntryByName(path.substring(rootPathWithSlash.length()), true);
    }
    for (FileSystemEntry s : children) {
      if (s instanceof FileSystemRoot) {
        FileSystemRoot subroot = (FileSystemRoot) s;
        FileSystemEntry e = subroot.getByAbsolutePath(path);
        if (e != null) return e;
      }
    }
    return null;
  }
}
