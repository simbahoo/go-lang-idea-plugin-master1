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

package com.goide.psi.impl;

import com.goide.GoConstants;
import com.goide.psi.*;
import com.goide.sdk.GoSdkUtil;
import com.goide.util.GoUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.OrderedSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.goide.psi.impl.GoPsiImplUtil.*;

public class GoReference extends PsiPolyVariantReferenceBase<GoReferenceExpressionBase> {
  public static final Key<List<? extends PsiElement>> IMPORT_USERS = Key.create("IMPORT_USERS");
  public static final Key<String> ACTUAL_NAME = Key.create("ACTUAL_NAME");
  private static final Key<Object> POINTER = Key.create("POINTER");
  private static final Key<Object> DONT_PROCESS_METHODS = Key.create("DONT_PROCESS_METHODS");

  private static final ResolveCache.PolyVariantResolver<PsiPolyVariantReferenceBase> MY_RESOLVER =
    new ResolveCache.PolyVariantResolver<PsiPolyVariantReferenceBase>() {
      @NotNull
      @Override
      public ResolveResult[] resolve(@NotNull PsiPolyVariantReferenceBase psiPolyVariantReferenceBase, boolean incompleteCode) {
        return ((GoReference)psiPolyVariantReferenceBase).resolveInner();
      }
    };

  public GoReference(@NotNull GoReferenceExpressionBase o) {
    super(o, TextRange.from(o.getIdentifier().getStartOffsetInParent(), o.getIdentifier().getTextLength()));
  }

  @Nullable
  static PsiFile getContextFile(@NotNull ResolveState state) {
    PsiElement element = getContextElement(state);
    return element != null ? element.getContainingFile() : null;
  }

  @NotNull
  private ResolveResult[] resolveInner() {
    if (!myElement.isValid()) return ResolveResult.EMPTY_ARRAY;
    Collection<ResolveResult> result = new OrderedSet<ResolveResult>();
    processResolveVariants(createResolveProcessor(result, myElement));
    return result.toArray(new ResolveResult[result.size()]);
  }
  
  @Override
  public boolean isReferenceTo(@NotNull PsiElement element) {
    return GoUtil.couldBeReferenceTo(element, myElement) && super.isReferenceTo(element);
  }

  @NotNull
  private PsiElement getIdentifier() {
    return myElement.getIdentifier();
  }

  @NotNull
  static GoScopeProcessor createResolveProcessor(@NotNull final Collection<ResolveResult> result, @NotNull final GoReferenceExpressionBase o) {
    return new GoScopeProcessor() {
      @Override
      public boolean execute(@NotNull PsiElement element, @NotNull ResolveState state) {
        if (element.equals(o)) return !result.add(new PsiElementResolveResult(element));
        String name = ObjectUtils.chooseNotNull(state.get(ACTUAL_NAME), 
                                                element instanceof PsiNamedElement ? ((PsiNamedElement)element).getName() : null);
        if (name != null && o.getIdentifier().textMatches(name)) {
          result.add(new PsiElementResolveResult(element));
          return false;
        }
        return true;
      }
    };
  }

  @Override
  @NotNull
  public ResolveResult[] multiResolve(boolean incompleteCode) {
    if (!myElement.isValid()) return ResolveResult.EMPTY_ARRAY;
    return ResolveCache.getInstance(myElement.getProject()).resolveWithCaching(this, MY_RESOLVER, false, false);
  }

  @NotNull
  @Override
  public Object[] getVariants() {
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  public boolean processResolveVariants(@NotNull GoScopeProcessor processor) {
    PsiFile file = myElement.getContainingFile();
    if (!(file instanceof GoFile)) return false;
    ResolveState state = createContextOnElement(myElement);
    GoReferenceExpressionBase qualifier = myElement.getQualifier();
    return qualifier != null
           ? processQualifierExpression((GoFile)file, qualifier, processor, state)
           : processUnqualifiedResolve((GoFile)file, processor, state);
  }

  private boolean processQualifierExpression(@NotNull GoFile file,
                                             @NotNull GoReferenceExpressionBase qualifier,
                                             @NotNull GoScopeProcessor processor,
                                             @NotNull ResolveState state) {
    PsiReference reference = qualifier.getReference();
    PsiElement target = reference != null ? reference.resolve() : null;
    if (target == null) return false;
    if (target == qualifier) return processor.execute(myElement, state);
    if (target instanceof GoImportSpec) {
      if (((GoImportSpec)target).isCImport()) return processor.execute(myElement, state);
      target = ((GoImportSpec)target).getImportString().resolve();
    }
    if (target instanceof PsiDirectory && !processDirectory((PsiDirectory)target, file, null, processor, state, false)) return false;
    if (target instanceof GoTypeOwner) {
      GoType type = typeOrParameterType((GoTypeOwner)target, createContextOnElement(myElement));
      if (type instanceof GoCType) return processor.execute(myElement, state);
      if (type != null) {
        if (!processGoType(type, processor, state)) return false;
        GoTypeReferenceExpression ref = getTypeRefExpression(type);
        if (ref != null && ref.resolve() == ref) return processor.execute(myElement, state); // a bit hacky resolve for: var a C.foo; a.b
      }
    }
    return true;
  }

  @Nullable
  private static GoTypeReferenceExpression getTypeRefExpression(@NotNull GoType type) {
    if (type instanceof GoPointerType) {
      GoType inner = ((GoPointerType)type).getType();
      return inner == null ? null : inner.getTypeReferenceExpression();
    }
    return type.getTypeReferenceExpression();
  }

  private boolean processGoType(@NotNull final GoType type, @NotNull final GoScopeProcessor processor, @NotNull final ResolveState state) {
    Boolean result = RecursionManager.doPreventingRecursion(type, true, new Computable<Boolean>() {
      @Override
      public Boolean compute() {
        if (type instanceof GoParType) return processGoType(((GoParType)type).getActualType(), processor, state);
        if (!processExistingType(type, processor, state)) return false;
        if (type instanceof GoPointerType) {
          if (!processPointer((GoPointerType)type, processor, state.put(POINTER, true))) return false;
          GoType pointer = ((GoPointerType)type).getType();
          if (pointer instanceof GoPointerType) {
            return processPointer((GoPointerType)pointer, processor, state.put(POINTER, true));
          }
        }
        return processTypeRef(type, processor, state);
      }
    });
    return Boolean.TRUE.equals(result);
  }

  private boolean processPointer(@NotNull GoPointerType type, @NotNull GoScopeProcessor processor, @NotNull ResolveState state) {
    GoType pointer = type.getType();
    return pointer == null || processExistingType(pointer, processor, state) && processTypeRef(pointer, processor, state);
  }

  private boolean processTypeRef(@Nullable GoType type, @NotNull GoScopeProcessor processor, @NotNull ResolveState state) {
    if (type == null) {
      return true;
    }
    if (builtin(type)) {
      // do not process builtin types like 'int int' or 'string string'
      return true;
    }
    return processInTypeRef(type.getTypeReferenceExpression(), processor, state);
  }

  private boolean processExistingType(@NotNull GoType type, @NotNull GoScopeProcessor processor, @NotNull ResolveState state) {
    PsiFile file = type.getContainingFile();
    if (!(file instanceof GoFile)) return true;
    PsiFile myFile = ObjectUtils.notNull(getContextFile(state), myElement.getContainingFile());
    if (!(myFile instanceof GoFile) || !allowed(file, myFile, ModuleUtilCore.findModuleForPsiElement(myFile))) return true;

    boolean localResolve = isLocalResolve(myFile, file);

    GoTypeSpec parent = getTypeSpecSafe(type);
    boolean canProcessMethods = state.get(DONT_PROCESS_METHODS) == null;
    if (canProcessMethods && parent != null && !processNamedElements(processor, state, parent.getMethods(), localResolve, true)) return false;

    if (type instanceof GoSpecType) {
      type = type.getUnderlyingType();
    }
    if (type instanceof GoStructType) {
      GoScopeProcessorBase delegate = createDelegate(processor);
      type.processDeclarations(delegate, ResolveState.initial(), null, myElement);
      List<GoTypeReferenceExpression> interfaceRefs = ContainerUtil.newArrayList();
      List<GoTypeReferenceExpression> structRefs = ContainerUtil.newArrayList();
      for (GoFieldDeclaration d : ((GoStructType)type).getFieldDeclarationList()) {
        if (!processNamedElements(processor, state, d.getFieldDefinitionList(), localResolve)) return false;
        GoAnonymousFieldDefinition anon = d.getAnonymousFieldDefinition();
        if (anon != null) {
          (anon.getMul() != null ? structRefs : interfaceRefs).add(anon.getTypeReferenceExpression());
          if (!processNamedElements(processor, state, ContainerUtil.createMaybeSingletonList(anon), localResolve)) return false;
        }
      }
      if (!processCollectedRefs(interfaceRefs, processor, state.put(POINTER, null))) return false;
      if (!processCollectedRefs(structRefs, processor, state)) return false;
    }
    else if (state.get(POINTER) == null && type instanceof GoInterfaceType) {
      if (!processNamedElements(processor, state, ((GoInterfaceType)type).getMethods(), localResolve, true)) return false;
      if (!processCollectedRefs(((GoInterfaceType)type).getBaseTypesReferences(), processor, state)) return false;
    }
    else if (type instanceof GoFunctionType) {
      GoSignature signature = ((GoFunctionType)type).getSignature();
      GoResult result = signature != null ? signature.getResult() : null;
      GoType resultType = result != null ? result.getType() : null;
      if (resultType != null && !processGoType(resultType, processor, state)) return false;
    }
    return true;
  }

  public static boolean isLocalResolve(@NotNull PsiFile originFile, @NotNull PsiFile externalFile) {
    if (!(originFile instanceof GoFile) || !(externalFile instanceof GoFile)) return false;
    GoFile o1 = (GoFile)originFile.getOriginalFile();
    GoFile o2 = (GoFile)externalFile.getOriginalFile();
    return Comparing.equal(o1.getImportPath(false), o2.getImportPath(false)) 
           && Comparing.equal(o1.getPackageName(), o2.getPackageName());
  }

  private boolean processCollectedRefs(@NotNull List<GoTypeReferenceExpression> refs,
                                       @NotNull GoScopeProcessor processor,
                                       @NotNull ResolveState state) {
    for (GoTypeReferenceExpression ref : refs) {
      if (!processInTypeRef(ref, processor, state)) return false;
    }
    return true;
  }

  private boolean processInTypeRef(@Nullable GoTypeReferenceExpression e, @NotNull GoScopeProcessor processor, @NotNull ResolveState state) {
    PsiElement resolve = e != null ? e.resolve() : null;
    if (resolve instanceof GoTypeOwner) {
      GoType type = ((GoTypeOwner)resolve).getGoType(state);
      if (type == null) return true;
      if (!processGoType(type, processor, state)) return false;
      if (type instanceof GoSpecType) {
        GoType inner = ((GoSpecType)type).getType();
        if (inner instanceof GoPointerType && state.get(POINTER) != null) return true;
        if (!processGoType(inner, processor, state.put(DONT_PROCESS_METHODS, true))) return false;
      }
      return true;
    }
    return true;
  }

  @Nullable
  private static String getPath(@Nullable PsiFile file) {
    if (file == null) return null;
    VirtualFile virtualFile = file.getOriginalFile().getVirtualFile();
    return virtualFile == null ? null : virtualFile.getPath();
  }

  protected static boolean processDirectory(@Nullable PsiDirectory dir,
                                            @Nullable GoFile file,
                                            @Nullable String packageName,
                                            @NotNull GoScopeProcessor processor,
                                            @NotNull ResolveState state,
                                            boolean localProcessing) {
    if (dir == null) return true;
    String filePath = getPath(file);
    Module module = file != null ? ModuleUtilCore.findModuleForPsiElement(file) : null;
    for (PsiFile f : dir.getFiles()) {
      if (!(f instanceof GoFile) || Comparing.equal(getPath(f), filePath)) continue;
      if (packageName != null && !packageName.equals(((GoFile)f).getPackageName())) continue;
      if (allowed(f, file, module) && !processFileEntities((GoFile)f, processor, state, localProcessing)) return false;
    }
    return true;
  }

  private boolean processUnqualifiedResolve(@NotNull GoFile file,
                                            @NotNull GoScopeProcessor processor,
                                            @NotNull ResolveState state) {
    // todo: rewrite with qualification not with siblings
    GoReceiverType receiverType = PsiTreeUtil.getPrevSiblingOfType(myElement, GoReceiverType.class);
    if (receiverType != null) {
      return processGoType(receiverType, processor, state);
    }

    if (getIdentifier().textMatches("_")) return processor.execute(myElement, state);

    PsiElement parent = myElement.getParent();

    if (parent instanceof GoSelectorExpr) {
      boolean result = processSelector((GoSelectorExpr)parent, processor, state, myElement);
      if (processor.isCompletion()) return result;
      if (!result || prevDot(myElement)) return false;
    }

    PsiElement grandPa = parent.getParent();
    if (grandPa instanceof GoSelectorExpr && !processSelector((GoSelectorExpr)grandPa, processor, state, parent)) return false;
    
    if (prevDot(parent)) return false;

    if (!processBlock(processor, state, true)) return false;
    if (!processReceiver(processor, state, true)) return false;
    if (!processImports(file, processor, state, myElement)) return false;
    if (!processFileEntities(file, processor, state, true)) return false;
    if (!processDirectory(file.getOriginalFile().getParent(), file, file.getPackageName(), processor, state, true)) return false;
    return processBuiltin(processor, state, myElement);
  }

  private boolean processReceiver(@NotNull GoScopeProcessor processor, @NotNull ResolveState state, boolean localResolve) {
    GoScopeProcessorBase delegate = createDelegate(processor);
    GoMethodDeclaration method = PsiTreeUtil.getParentOfType(myElement, GoMethodDeclaration.class);
    GoReceiver receiver = method != null ? method.getReceiver() : null;
    if (receiver == null) return true;
    receiver.processDeclarations(delegate, ResolveState.initial(), null, myElement);
    return processNamedElements(processor, state, delegate.getVariants(), localResolve);
  }

  private boolean processBlock(@NotNull GoScopeProcessor processor, @NotNull ResolveState state, boolean localResolve) {
    GoScopeProcessorBase delegate = createDelegate(processor);
    ResolveUtil.treeWalkUp(myElement, delegate);
    return processNamedElements(processor, state, delegate.getVariants(), localResolve);
  }

  private static boolean processBuiltin(@NotNull GoScopeProcessor processor,
                                        @NotNull ResolveState state,
                                        @NotNull GoCompositeElement element) {
    GoFile builtin = GoSdkUtil.findBuiltinFile(element);
    return builtin == null || processFileEntities(builtin, processor, state, true);
  }

  static boolean processImports(@NotNull GoFile file,
                                @NotNull GoScopeProcessor processor,
                                @NotNull ResolveState state,
                                @NotNull GoCompositeElement element) {
    for (Map.Entry<String, Collection<GoImportSpec>> entry : file.getImportMap().entrySet()) {
      for (GoImportSpec o : entry.getValue()) {
        if (o.isForSideEffects()) continue;
        
        GoImportString importString = o.getImportString();
        if (o.isDot()) {
          PsiDirectory implicitDir = importString.resolve();
          boolean resolved = !processDirectory(implicitDir, file, null, processor, state, false);
          if (resolved && !processor.isCompletion()) {
            putIfAbsent(o, element);
          }
          if (resolved) return false;
        }
        else {
          if (o.getAlias() == null) {
            PsiDirectory resolve = importString.resolve();
            if (resolve != null && !processor.execute(resolve, state.put(ACTUAL_NAME, entry.getKey()))) return false;
          }
          // todo: multi-resolve into appropriate package clauses
          if (!processor.execute(o, state.put(ACTUAL_NAME, entry.getKey()))) return false;
        }
      }
    }
    return true;
  }

  private boolean processSelector(@NotNull GoSelectorExpr parent,
                                  @NotNull GoScopeProcessor processor,
                                  @NotNull ResolveState state,
                                  @Nullable PsiElement another) {
    List<GoExpression> list = parent.getExpressionList();
    if (list.size() > 1 && list.get(1).isEquivalentTo(another)) {
      GoExpression e = list.get(0);
      List<GoReferenceExpression> refs = ContainerUtil.newArrayList(PsiTreeUtil.findChildrenOfType(e, GoReferenceExpression.class));
      GoExpression o = refs.size() > 1 ? refs.get(refs.size() - 1) : e;
      PsiReference ref = o.getReference();
      PsiElement resolve = ref != null ? ref.resolve() : null;
      if (resolve == o) return processor.execute(myElement, state); // var c = C.call(); c.a.b.d;
      GoType type = e.getGoType(createContextOnElement(myElement));
      if (type != null && !processGoType(type, processor, state)) return false;
    }
    return true;
  }

  @NotNull
  private GoVarProcessor createDelegate(@NotNull GoScopeProcessor processor) {
    return new GoVarProcessor(getIdentifier(), myElement, processor.isCompletion(), true) {
      @Override
      protected boolean crossOff(@NotNull PsiElement e) {
        if (e instanceof GoFieldDefinition) return true;
        return super.crossOff(e) && !(e instanceof GoTypeSpec);
      }
    };
  }

  private static boolean processFileEntities(@NotNull GoFile file,
                                             @NotNull GoScopeProcessor processor,
                                             @NotNull final ResolveState state,
                                             boolean localProcessing) {
    if (!processNamedElements(processor, state, file.getConstants(), new Condition<GoNamedElement>() {
        @Override
        public boolean value(@NotNull GoNamedElement o) {
          return !Comparing.equal(GoConstants.IOTA, o.getName()) ||
                 !builtin(o) ||
                 PsiTreeUtil.getParentOfType(getContextElement(state), GoConstSpec.class) != null;
        }
      }, localProcessing, false)) return false;
    if (!processNamedElements(processor, state, file.getVars(), localProcessing)) return false;
    Condition<GoNamedElement> dontProcessInit = new Condition<GoNamedElement>() {
      @Override
      public boolean value(@NotNull GoNamedElement o) {
        return o instanceof GoFunctionDeclaration && !Comparing.equal(o.getName(), GoConstants.INIT);
      }
    };
    if (!processNamedElements(processor, state, file.getFunctions(), dontProcessInit, localProcessing, false)) return false;
    return processNamedElements(processor, state, file.getTypes(), localProcessing);
  }

  @NotNull
  @Override
  public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
    getIdentifier().replace(GoElementFactory.createIdentifierFromText(myElement.getProject(), newElementName));
    return myElement;
  }

  private static void putIfAbsent(@NotNull PsiElement importElement, @NotNull PsiElement usage) {
    List<PsiElement> newList = ContainerUtil.newSmartList(usage);
    List<? extends PsiElement> list = importElement.getUserData(IMPORT_USERS);
    if (list != null) {
      newList.addAll(list);
    }
    importElement.putUserData(IMPORT_USERS, newList);
  }
}