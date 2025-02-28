package com.google.android.diskusage.ui;

import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Process;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import android.app.AlertDialog;
import com.google.android.diskusage.R;
import com.google.android.diskusage.databinding.AboutDialogBinding;
import com.google.android.diskusage.datasource.SearchManager;
import com.google.android.diskusage.filesystem.entity.FileSystemEntry;
import com.google.android.diskusage.filesystem.entity.FileSystemSpecial;
import com.google.android.diskusage.filesystem.entity.FileSystemSuperRoot;
import com.google.android.diskusage.filesystem.mnt.MountPoint;
import com.google.android.diskusage.utils.AppIconCache;

import org.jetbrains.annotations.Contract;

public abstract class DiskUsageMenu {
  public final DiskUsage diskusage;
  protected String searchPattern;
  private FileSystemEntry selectedEntity;
  public FileSystemSuperRoot masterRoot;
  SearchManager searchManager = new SearchManager(this);

  protected MenuItem searchMenuItem;
  protected MenuItem showMenuItem;
  protected MenuItem rescanMenuItem;
  protected MenuItem deleteMenuItem;
  protected MenuItem rendererMenuItem;
  protected MenuItem aboutMenuItem;

  public DiskUsageMenu(DiskUsage diskusage) {
    this.diskusage = diskusage;
  }

  @NonNull
  @Contract("_ -> new")
  public static DiskUsageMenu getInstance(DiskUsage diskusage) {
    return new DiskUsageMenuHoneycomb(diskusage);
  }

  public abstract void onCreate();
  public abstract boolean readyToFinish();
  public abstract void searchRequest();
  public abstract MenuItem makeSearchMenuEntry(Menu menu);

  public final void onSaveInstanceState(@NonNull Bundle outState) {
    outState.putString("search", searchPattern);
  }

  public final void onRestoreInstanceState(@NonNull Bundle inState) {
    searchPattern = inState.getString("search");
  }

  public void wrapAndSetContentView(View view, FileSystemSuperRoot newRoot) {
    this.masterRoot = newRoot;
    updateMenu();
  }

  public void applyPattern(String searchQuery) {
    if (searchQuery == null || masterRoot == null) return;

    if (searchQuery.length() == 0) {
      searchManager.cancelSearch();
      finishedSearch(masterRoot, searchQuery);
    } else {
      searchManager.search(searchQuery);
    }
  }

  public boolean finishedSearch(FileSystemSuperRoot newRoot, String searchQuery) {
    boolean matched = newRoot != null;
    if (!matched) newRoot = masterRoot;
    diskusage.applyPatternNewRoot(newRoot, searchQuery);
    return matched;
  }

  public void addRescanMenuEntry(@NonNull Menu menu) {
    menu.add(getString(R.string.button_rescan))
    .setOnMenuItemClickListener(item -> {
      diskusage.rescan();
      return true;
    });
  }

  public void update(FileSystemEntry position) {
    this.selectedEntity = position;
    updateMenu();
  }

  private String getString(int id) {
    return diskusage.getString(id);
  }

  public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
    menu.clear();
    searchMenuItem = makeSearchMenuEntry(menu);

    showMenuItem = menu.add(getString(R.string.button_show));
    showMenuItem.setOnMenuItemClickListener(item -> {
      if (selectedEntity != null) {
        diskusage.view(selectedEntity);
      }
      return true;
    });
    rescanMenuItem = menu.add(getString(R.string.button_rescan));
    rescanMenuItem.setOnMenuItemClickListener(item -> {
      diskusage.rescan();
      return true;
    });

    deleteMenuItem = menu.add(getString(R.string.button_delete));
    deleteMenuItem.setOnMenuItemClickListener(item -> {
      diskusage.askForDeletion(selectedEntity);
      return true;
    });

    rendererMenuItem = menu.add("Renderer");
    rendererMenuItem.setVisible(true);
    rendererMenuItem.setOnMenuItemClickListener(item -> {
      diskusage.rendererManager.switchRenderer(masterRoot);
      return true;
    });

    aboutMenuItem = menu.add(R.string.action_about);
    aboutMenuItem.setOnMenuItemClickListener(item -> {
      final AboutDialogBinding binding =
              AboutDialogBinding.inflate(LayoutInflater.from(diskusage), null, false);
      binding.sourceCode.setMovementMethod(LinkMovementMethod.getInstance());
      binding.sourceCode.setText(Html.fromHtml(diskusage.getString(
              R.string.about_view_source_code,
              "<b><a href=\"https://github.com/IvanVolosyuk/diskusage\">GitHub</a></b>"
              ))
      );
      binding.icon.setImageBitmap(
              AppIconCache.getOrLoadBitmap(
                      diskusage,
                      diskusage.getApplicationInfo(),
                      Process.myUid() / 100000,
                      diskusage.getResources().getDimensionPixelSize(R.dimen.default_app_icon_size)
              )
      );
      try {
        binding.versionName.setText(diskusage.getPackageManager().getPackageInfo(
                diskusage.getPackageName(), 0
        ).versionName);
      } catch (PackageManager.NameNotFoundException e) {
        e.printStackTrace();
      }
      new AlertDialog.Builder(diskusage)
              .setView(binding.getRoot())
              .show();
      return true;
    });

    updateMenu();
    return true;
  }

  private void updateMenu() {
    if (showMenuItem == null) return;

    if (diskusage.fileSystemState == null) {
      searchMenuItem.setEnabled(false);
      showMenuItem.setEnabled(false);
      rescanMenuItem.setEnabled(false);
      deleteMenuItem.setEnabled(false);
      rendererMenuItem.setEnabled(false);
      return;
    }

    if (diskusage.fileSystemState.sdcardIsEmpty()) {
      searchMenuItem.setEnabled(false);
      showMenuItem.setEnabled(false);
      rescanMenuItem.setEnabled(true);
      deleteMenuItem.setEnabled(false);
      rendererMenuItem.setEnabled(false);
    }

    rendererMenuItem.setEnabled(true);
    final boolean isGPU = diskusage.fileSystemState.isGPU();
    rendererMenuItem.setTitle(isGPU ? "Software Renderer" : "Hardware Renderer");

    rescanMenuItem.setEnabled(true);
    searchMenuItem.setEnabled(true);


    boolean view = !(selectedEntity == diskusage.fileSystemState.masterRoot.children[0]
                || selectedEntity instanceof FileSystemSpecial);
    showMenuItem.setEnabled(view);

    boolean fileOrNotSearching = searchPattern == null || selectedEntity.children == null;
    MountPoint mountPoint = MountPoint.getForKey(diskusage, diskusage.getKey());
    deleteMenuItem.setEnabled(view && selectedEntity.isDeletable()
        && fileOrNotSearching && mountPoint.isDeleteSupported());
  }
}
