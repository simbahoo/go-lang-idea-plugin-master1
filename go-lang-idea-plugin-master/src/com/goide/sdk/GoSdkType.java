/*
 * Copyright 2013-2015 Sergey Ignatov, Alexander Zolotov, Florin Patan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.goide.sdk;

import com.goide.GoConstants;
import com.goide.GoIcons;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;

public class GoSdkType extends SdkType {
  public GoSdkType() {
    super(GoConstants.SDK_TYPE_ID);
  }

  @NotNull
  public static GoSdkType getInstance() {
    return SdkType.findInstance(GoSdkType.class);
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return GoIcons.ICON;
  }

  @NotNull
  @Override
  public Icon getIconForAddAction() {
    return getIcon();
  }

  @Nullable
  @Override
  public String suggestHomePath() {
    VirtualFile suggestSdkDirectory = GoSdkUtil.suggestSdkDirectory();
    return suggestSdkDirectory != null ? suggestSdkDirectory.getPath() : null;
  }

  @Override
  public boolean isValidSdkHome(@NotNull String path) {
    GoSdkService.LOG.debug("Validating sdk path: " + path);
    String executablePath = GoSdkService.getGoExecutablePath(path);
    if (executablePath == null) {
      GoSdkService.LOG.debug("Go executable is not found: ");
      return false;
    }
    if (!new File(executablePath).canExecute()) {
      GoSdkService.LOG.debug("Go binary cannot be executed: " + path);
      return false;
    }
    if (getVersionString(path) != null) {
      GoSdkService.LOG.debug("Cannot retrieve version for sdk: " + path);
      return true;
    }
    return false;
  }

  @NotNull
  @Override
  public String adjustSelectedSdkHome(@NotNull String homePath) {
    return GoSdkUtil.adjustSdkPath(homePath);
  }

  @NotNull
  @Override
  public String suggestSdkName(@Nullable String currentSdkName, @NotNull String sdkHome) {
    String version = getVersionString(sdkHome);
    if (version == null) {
      return "Unknown Go version at " + sdkHome;
    }
    return "Go " + version;
  }

  @Nullable
  @Override
  public String getVersionString(@NotNull String sdkHome) {
    return GoSdkUtil.retrieveGoVersion(sdkHome);
  }

  @Nullable
  @Override
  public String getDefaultDocumentationUrl(@NotNull Sdk sdk) {
    return null;
  }

  @Nullable
  @Override
  public AdditionalDataConfigurable createAdditionalDataConfigurable(@NotNull SdkModel sdkModel, @NotNull SdkModificator sdkModificator) {
    return null;
  }

  @Override
  public void saveAdditionalData(@NotNull SdkAdditionalData additionalData, @NotNull Element additional) {
  }

  @NotNull
  @NonNls
  @Override
  public String getPresentableName() {
    return "Go SDK";
  }

  @Override
  public void setupSdkPaths(@NotNull Sdk sdk) {
    String versionString = sdk.getVersionString();
    if (versionString == null) throw new RuntimeException("SDK version is not defined");
    SdkModificator modificator = sdk.getSdkModificator();
    String path = sdk.getHomePath();
    if (path == null) return;
    modificator.setHomePath(path);

    for (VirtualFile file : GoSdkUtil.getSdkDirectoriesToAttach(path, versionString)) {
      modificator.addRoot(file, OrderRootType.CLASSES);
      modificator.addRoot(file, OrderRootType.SOURCES);
    }
    modificator.commitChanges();
  }
}
