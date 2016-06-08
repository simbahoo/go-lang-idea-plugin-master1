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

package com.goide.completion;

import com.goide.project.GoVendoringUtil;
import com.goide.psi.*;
import com.goide.psi.impl.GoPsiImplUtil;
import com.goide.psi.impl.GoTypeReference;
import com.goide.runconfig.testing.GoTestFinder;
import com.goide.stubs.index.GoIdFilter;
import com.goide.util.GoUtil;
import com.intellij.codeInsight.completion.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.util.ProcessingContext;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.IdFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.goide.completion.GoCompletionUtil.createPrefixMatcher;
import static com.goide.psi.impl.GoPsiImplUtil.prevDot;
import static com.goide.stubs.index.GoAllPublicNamesIndex.ALL_PUBLIC_NAMES;
import static com.intellij.patterns.PlatformPatterns.psiElement;

public class GoAutoImportCompletionContributor extends CompletionContributor {
  public GoAutoImportCompletionContributor() {
    extend(CompletionType.BASIC, inGoFile(), new CompletionProvider<CompletionParameters>() {
      @Override
      protected void addCompletions(@NotNull CompletionParameters parameters,
                                    ProcessingContext context,
                                    @NotNull CompletionResultSet result) {
        PsiElement position = parameters.getPosition();
        PsiElement parent = position.getParent();
        if (prevDot(parent)) return;
        PsiFile psiFile = parameters.getOriginalFile();
        if (!(psiFile instanceof GoFile && parent instanceof GoReferenceExpressionBase)) return;
        GoFile file = (GoFile)psiFile;

        result = adjustMatcher(parameters, result, parent);
        PrefixMatcher matcher = result.getPrefixMatcher();
        if (parameters.getInvocationCount() < 2 && matcher.getPrefix().isEmpty()) {
          result.restartCompletionOnPrefixChange(StandardPatterns.string().longerThan(0));
          return;
        }

        GoReferenceExpressionBase qualifier = ((GoReferenceExpressionBase)parent).getQualifier();
        if (qualifier != null && qualifier.getReference() != null && qualifier.getReference().resolve() != null) return;

        ArrayList<ElementProcessor> processors = ContainerUtil.newArrayList();
        if (parent instanceof GoReferenceExpression && !GoPsiImplUtil.isUnaryBitAndExpression(parent)) {
          processors.add(new FunctionsProcessor());
          processors.add(new VariablesAndConstantsProcessor());
        }
        if (parent instanceof GoReferenceExpression || parent instanceof GoTypeReferenceExpression) {
          processors.add(new TypesProcessor(parent));
        }
        if (processors.isEmpty()) return;

        Module module = ModuleUtilCore.findModuleForPsiElement(psiFile);
        NamedElementProcessor processor = new NamedElementProcessor(processors, file, result, module);
        Project project = position.getProject();
        GlobalSearchScope scope = new GoUtil.ExceptTestsScope(GoUtil.goPathResolveScope(file));
        VirtualFile containingDirectory = file.getVirtualFile().getParent();
        if (containingDirectory != null) {
          scope = new GoUtil.ExceptChildOfDirectory(containingDirectory, scope, GoTestFinder.getTestTargetPackage(file));
        }
        IdFilter idFilter = GoIdFilter.getProductionFilter(project);
        Set<String> sortedKeys = collectAndSortAllPublicProductionNames(matcher, scope, idFilter, file);
        for (String name : sortedKeys) {
          processor.setName(name);
          for (GoNamedElement element : StubIndex.getElements(ALL_PUBLIC_NAMES, name, project, scope, idFilter, GoNamedElement.class)) {
            if (!processor.process(element)) {
              break;
            }
          }
        }
      }

      private CompletionResultSet adjustMatcher(@NotNull CompletionParameters parameters,
                                                @NotNull CompletionResultSet result,
                                                @NotNull PsiElement parent) {
        int startOffset = parent.getTextRange().getStartOffset();
        String newPrefix = parameters.getEditor().getDocument().getText(TextRange.create(startOffset, parameters.getOffset()));
        return result.withPrefixMatcher(createPrefixMatcher(newPrefix));
      }
    });
  }

  @NotNull
  private static Set<String> collectAndSortAllPublicProductionNames(@NotNull PrefixMatcher matcher,
                                                                    @NotNull GlobalSearchScope scope, 
                                                                    @Nullable IdFilter idFilter,
                                                                    @NotNull GoFile file) {
    String prefix = matcher.getPrefix();
    final boolean emptyPrefix = prefix.isEmpty();

    Set<String> packagesWithAliases = ContainerUtil.newHashSet();
    if (!emptyPrefix) {
      for (Map.Entry<String, Collection<GoImportSpec>> entry : file.getImportMap().entrySet()) {
        for (GoImportSpec spec : entry.getValue()) {
          String alias = spec.getAlias();
          if (spec.isDot() || alias != null) {
            packagesWithAliases.add(entry.getKey());
            break;
          }
        }
      }
    }

    Set<String> allNames = ContainerUtil.newTroveSet();
    StubIndex.getInstance().processAllKeys(ALL_PUBLIC_NAMES, new CancellableCollectProcessor<String>(allNames) {
      @Override
      protected boolean accept(String s) {
        return emptyPrefix || matcher.prefixMatches(s) || packagesWithAliases.contains(substringBefore(s, '.'));
      }
    }, scope, idFilter);

    if (emptyPrefix) {
      return allNames;
    }

    List<String> sorted = ContainerUtil.sorted(allNames, String.CASE_INSENSITIVE_ORDER);
    ProgressManager.checkCanceled();

    LinkedHashSet<String> result = ContainerUtil.newLinkedHashSet();
    for (String name : sorted) {
      ProgressManager.checkCanceled();
      if (matcher.isStartMatch(name)) {
        result.add(name);
      }
    }
    result.addAll(sorted);
    return result;
  }

  private static PsiElementPattern.Capture<PsiElement> inGoFile() {
    return psiElement().inFile(psiElement(GoFile.class));
  }

  @NotNull
  private static String substringBefore(@NotNull String s, char c) {
    int i = s.indexOf(c);
    if (i == -1) return s;
    return s.substring(0, i);
  }

  private static String substringAfter(@NotNull String s, char c) {
    int i = s.indexOf(c);
    if (i == -1) return "";
    return s.substring(i + 1);
  }

  @NotNull
  private static String replacePackageWithAlias(@NotNull String qualifiedName, @Nullable String alias) {
    return alias != null ? alias + "." + substringAfter(qualifiedName, '.') : qualifiedName;
  }

  private interface ElementProcessor {
    boolean process(@NotNull String name,
                    @NotNull GoNamedElement element,
                    @NotNull ExistingImportData importData,
                    @NotNull CompletionResultSet result);
    boolean isMine(@NotNull String name, @NotNull GoNamedElement element);
  }

  private static class VariablesAndConstantsProcessor implements ElementProcessor {
    @Override
    public boolean process(@NotNull String name,
                           @NotNull GoNamedElement element,
                           @NotNull ExistingImportData importData,
                           @NotNull CompletionResultSet result) {
      double priority = importData.exists ? GoCompletionUtil.VAR_PRIORITY : GoCompletionUtil.NOT_IMPORTED_VAR_PRIORITY;
      result.addElement(GoCompletionUtil.createVariableLikeLookupElement(element, replacePackageWithAlias(name, importData.alias),
                                                                         GoAutoImportInsertHandler.SIMPLE_INSERT_HANDLER, priority));
      return true;
    }

    @Override
    public boolean isMine(@NotNull String name, @NotNull GoNamedElement element) {
      return element instanceof GoVarDefinition || element instanceof GoConstDefinition;
    }
  }

  private static class FunctionsProcessor implements ElementProcessor {
    @Override
    public boolean process(@NotNull String name,
                           @NotNull GoNamedElement element,
                           @NotNull ExistingImportData importData,
                           @NotNull CompletionResultSet result) {
      GoFunctionDeclaration function = (GoFunctionDeclaration)element;
      double priority = importData.exists ? GoCompletionUtil.FUNCTION_PRIORITY : GoCompletionUtil.NOT_IMPORTED_FUNCTION_PRIORITY;
      result.addElement(GoCompletionUtil.createFunctionOrMethodLookupElement(function, replacePackageWithAlias(name, importData.alias),
                                                                             GoAutoImportInsertHandler.FUNCTION_INSERT_HANDLER, priority));
      return true;
    }

    @Override
    public boolean isMine(@NotNull String name, @NotNull GoNamedElement element) {
      return element instanceof GoFunctionDeclaration;
    }
  }

  private static class TypesProcessor implements ElementProcessor {
    @Nullable private final PsiElement myParent;

    public TypesProcessor(@Nullable PsiElement parent) {
      myParent = parent;
    }

    @Override
    public boolean process(@NotNull String name,
                           @NotNull GoNamedElement element,
                           @NotNull ExistingImportData importData,
                           @NotNull CompletionResultSet result) {
      GoTypeSpec spec = (GoTypeSpec)element;
      boolean forTypes = myParent instanceof GoTypeReferenceExpression;
      double priority;
      if (importData.exists) {
        priority = forTypes ? GoCompletionUtil.TYPE_PRIORITY : GoCompletionUtil.TYPE_CONVERSION;
      }
      else {
        priority = forTypes ? GoCompletionUtil.NOT_IMPORTED_TYPE_PRIORITY : GoCompletionUtil.NOT_IMPORTED_TYPE_CONVERSION;
      }

      String lookupString = replacePackageWithAlias(name, importData.alias);
      if (forTypes) {
        result.addElement(GoCompletionUtil.createTypeLookupElement(spec, lookupString, GoAutoImportInsertHandler.SIMPLE_INSERT_HANDLER,
                                                                   importData.importPath, priority));
      }
      else {
        result.addElement(GoCompletionUtil.createTypeConversionLookupElement(spec, lookupString,
                                                                             GoAutoImportInsertHandler.TYPE_CONVERSION_INSERT_HANDLER,
                                                                             importData.importPath, priority));
      }
      return true;
    }

    @Override
    public boolean isMine(@NotNull String name, @NotNull GoNamedElement element) {
      if (myParent != null && element instanceof GoTypeSpec) {
        PsiReference reference = myParent.getReference();
        return !(reference instanceof GoTypeReference) || ((GoTypeReference)reference).allowed((GoTypeSpec)element);
      }
      return false;
    }
  }

  private static class NamedElementProcessor implements Processor<GoNamedElement> {
    @NotNull private final Collection<ElementProcessor> myProcessors;
    @NotNull private final CompletionResultSet myResult;
    @NotNull private String myName = "";
    @NotNull private final Map<String, GoImportSpec> myImportedPackages;
    @Nullable private final Module myModule;
    private final boolean myVendoringEnabled;

    public NamedElementProcessor(@NotNull Collection<ElementProcessor> processors,
                                 @NotNull GoFile contextFile,
                                 @NotNull CompletionResultSet result,
                                 @Nullable Module module) {
      myProcessors = processors;
      myVendoringEnabled = GoVendoringUtil.isVendoringEnabled(module);
      myImportedPackages = contextFile.getImportedPackagesMap();
      myModule = module;
      myResult = result;
    }

    public void setName(@NotNull String name) {
      myName = name;
    }

    @Override
    public boolean process(@NotNull GoNamedElement element) {
      ProgressManager.checkCanceled();
      Boolean allowed = null;
      ExistingImportData importData = null;
      for (ElementProcessor processor : myProcessors) {
        if (processor.isMine(myName, element)) {
          importData = cachedImportData(element, importData);
          allowed = cachedAllowed(element, allowed);
          if (allowed == Boolean.FALSE || importData.isDot) break;
          if (!processor.process(myName, element, importData, myResult)) {
            return false;
          }
        }
      }
      return true;
    }

    @NotNull
    private Boolean cachedAllowed(@NotNull GoNamedElement element, @Nullable Boolean existingValue) {
      if (existingValue != null) return existingValue;
      return GoPsiImplUtil.canBeAutoImported(element.getContainingFile(), false, myModule);
    }

    @NotNull
    private ExistingImportData cachedImportData(@NotNull GoNamedElement element, @Nullable ExistingImportData existingValue) {
      if (existingValue != null) return existingValue;

      GoFile declarationFile = element.getContainingFile();
      String importPath = declarationFile.getImportPath(myVendoringEnabled);
      GoImportSpec existingImport = myImportedPackages.get(importPath);

      boolean exists = existingImport != null;
      boolean isDot = exists && existingImport.isDot();
      String alias = existingImport != null ? existingImport.getAlias() : null;
      return new ExistingImportData(exists, isDot, alias, importPath);
    }
  }

  private static class ExistingImportData {
    public final boolean exists;
    public final boolean isDot;
    public final String alias;
    public final String importPath;

    private ExistingImportData(boolean exists, boolean isDot, String packageName, String importPath) {
      this.exists = exists;
      this.isDot = isDot;
      alias = packageName;
      this.importPath = importPath;
    }
  }
}
