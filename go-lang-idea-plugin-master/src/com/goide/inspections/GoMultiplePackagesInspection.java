/*
 * Copyright 2013-2016 Sergey Ignatov, Alexander Zolotov, Florin Patan
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

package com.goide.inspections;

import com.goide.GoConstants;
import com.goide.psi.GoFile;
import com.goide.psi.GoPackageClause;
import com.goide.quickfix.GoMultiplePackagesQuickFix;
import com.goide.sdk.GoPackageUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.ide.scratch.ScratchFileType;
import com.intellij.psi.PsiDirectory;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class GoMultiplePackagesInspection extends GoInspectionBase {
  @Override
  protected void checkFile(@NotNull GoFile file, @NotNull ProblemsHolder problemsHolder) {
    if (((ScratchFileType)ScratchFileType.INSTANCE).isMyFileType(file.getVirtualFile())) return;
    GoPackageClause packageClause = file.getPackage();
    if (packageClause != null) {
      String packageName = file.getPackageName();
      if (packageName == null || packageName.equals(GoConstants.DOCUMENTATION)) return;
      PsiDirectory dir = file.getContainingDirectory();
      Collection<String> packages = GoPackageUtil.getAllPackagesInDirectory(dir, null, true);
      packages.remove(GoConstants.DOCUMENTATION);
      if (packages.size() > 1) {
        Collection<LocalQuickFix> fixes = ContainerUtil.newArrayList();
        if (problemsHolder.isOnTheFly()) {
          fixes.add(new GoMultiplePackagesQuickFix(packageClause, packageName, packages, true));
        }
        else {
          for (String name : packages) {
            fixes.add(new GoMultiplePackagesQuickFix(packageClause, name, packages, false));
          }
        }
        problemsHolder.registerProblem(packageClause, "Multiple packages in directory", fixes.toArray(new LocalQuickFix[fixes.size()]));
      }
    }
  }
}
